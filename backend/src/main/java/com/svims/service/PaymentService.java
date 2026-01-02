package com.svims.service;

import com.svims.dto.PaymentDTO;
import com.svims.entity.Invoice;
import com.svims.entity.Payment;
import com.svims.repository.InvoiceRepository;
import com.svims.repository.PaymentRepository;
import com.svims.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Payment operations
 * Handles partial and full payments, updates invoice status
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final AuditService auditService;
    private final SecurityUtil securityUtil;

    /**
     * Get all payments based on user role:
     * - USER: Payments for their own invoices only
     * - MANAGER: No access to payments
     * - FINANCE: All payments
     * - ADMIN: All payments
     */
    public List<PaymentDTO> getAllPayments() {
        String currentUsername = securityUtil.getCurrentUsername();
        List<Payment> payments;

        if (securityUtil.isAdmin() || securityUtil.isFinance()) {
            // ADMIN and FINANCE can see all payments
            payments = paymentRepository.findAll();
        } else if (securityUtil.isUser()) {
            // USER can only see payments for their own invoices
            List<Invoice> userInvoices = invoiceRepository.findByCreatedBy(currentUsername);
            payments = paymentRepository.findAll().stream()
                    .filter(payment -> userInvoices.contains(payment.getInvoice()))
                    .collect(Collectors.toList());
        } else {
            // MANAGER has no access to payments
            throw new RuntimeException("Access denied: Managers cannot view payments");
        }

        return payments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get payment by ID with role-based access control
     */
    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        
        String currentUsername = securityUtil.getCurrentUsername();
        
        // Check access based on role
        if (!securityUtil.isAdmin() && !securityUtil.isFinance()) {
            if (securityUtil.isUser()) {
                // USER can only access payments for their own invoices
                Invoice invoice = payment.getInvoice();
                if (invoice.getCreatedBy() == null || !invoice.getCreatedBy().equals(currentUsername)) {
                    throw new RuntimeException("Access denied: You can only view payments for your own invoices");
                }
            } else {
                // MANAGER has no access
                throw new RuntimeException("Access denied: Managers cannot view payments");
            }
        }
        
        return convertToDTO(payment);
    }

    /**
     * Get payments by invoice ID with role-based access control
     */
    public List<PaymentDTO> getPaymentsByInvoiceId(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
        
        String currentUsername = securityUtil.getCurrentUsername();
        
        // Check access based on role
        if (!securityUtil.isAdmin() && !securityUtil.isFinance()) {
            if (securityUtil.isUser()) {
                // USER can only access payments for their own invoices
                if (invoice.getCreatedBy() == null || !invoice.getCreatedBy().equals(currentUsername)) {
                    throw new RuntimeException("Access denied: You can only view payments for your own invoices");
                }
            } else {
                // MANAGER has no access
                throw new RuntimeException("Access denied: Managers cannot view payments");
            }
        }
        
        return paymentRepository.findByInvoice(invoice).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Records a new payment (partial or full) for an invoice.
     * 
     * This method performs the following operations:
     * - Validates that the payment amount doesn't exceed the remaining invoice balance
     * - Creates a payment record with amount, payment method, transaction reference, and notes
     * - Calculates new total paid amount including this payment
     * - Updates invoice status to PAID if fully paid, or PARTIALLY_PAID if partial payment
     * - Creates audit log entry for tracking payment creation
     * 
     * Supports both partial and full payments. Multiple partial payments can be made
     * until the invoice is fully paid.
     * 
     * @param paymentDTO The payment data transfer object containing amount, invoice ID, payment details
     * @param userName The username of the user creating the payment (for audit trail)
     * @param request HTTP servlet request for capturing IP address in audit log
     * @return PaymentDTO containing the created payment record
     * @throws RuntimeException if invoice not found, payment amount exceeds remaining balance, or validation fails
     */
    @Transactional
    public PaymentDTO createPayment(PaymentDTO paymentDTO, String userName, HttpServletRequest request) {
        Invoice invoice = invoiceRepository.findById(paymentDTO.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + paymentDTO.getInvoiceId()));

        // Validate payment amount
        BigDecimal totalPaid = paymentRepository.getTotalPaidAmount(invoice);
        BigDecimal remainingAmount = invoice.getTotalAmount().subtract(totalPaid);
        
        if (paymentDTO.getAmount().compareTo(remainingAmount) > 0) {
            throw new RuntimeException("Payment amount exceeds remaining invoice amount. Remaining: " + remainingAmount);
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentDate(paymentDTO.getPaymentDate() != null ? 
                paymentDTO.getPaymentDate() : LocalDateTime.now());
        payment.setPaymentMethod(paymentDTO.getPaymentMethod());
        payment.setTransactionReference(paymentDTO.getTransactionReference());
        payment.setNotes(paymentDTO.getNotes());

        payment = paymentRepository.save(payment);

        // Update invoice status
        BigDecimal newTotalPaid = totalPaid.add(payment.getAmount());
        if (newTotalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
        } else {
            invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);

        // Log audit
        auditService.logAction(userName, "CREATE", "PAYMENT", payment.getId(),
                null, paymentDTO, "Payment created for invoice " + invoice.getId(), request);

        return convertToDTO(payment);
    }

    /**
     * Permanently deletes a payment record from the system.
     * 
     * This method:
     * - Removes the payment record from the database
     * - Recalculates the total paid amount for the associated invoice
     * - Updates invoice status based on remaining payments:
     *   - APPROVED if no payments remain
     *   - PARTIALLY_PAID if partial payments still exist
     * - Creates audit log entry before deletion
     * 
     * Should be used with caution as deletion is irreversible.
     * 
     * @param id The unique identifier of the payment to delete
     * @param userName The username of the user deleting the payment (for audit trail)
     * @param request HTTP servlet request for capturing IP address in audit log
     * @throws RuntimeException if payment not found
     */
    @Transactional
    public void deletePayment(Long id, String userName, HttpServletRequest request) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + id));
        
        Invoice invoice = payment.getInvoice();
        PaymentDTO oldDTO = convertToDTO(payment);
        
        paymentRepository.delete(payment);

        // Recalculate invoice status
        BigDecimal totalPaid = paymentRepository.getTotalPaidAmount(invoice);
        if (totalPaid.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(Invoice.InvoiceStatus.APPROVED);
        } else if (totalPaid.compareTo(invoice.getTotalAmount()) < 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);

        // Log audit
        auditService.logAction(userName, "DELETE", "PAYMENT", id,
                oldDTO, null, "Payment deleted", request);
    }

    private PaymentDTO convertToDTO(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setInvoiceId(payment.getInvoice().getId());
        dto.setAmount(payment.getAmount());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setTransactionReference(payment.getTransactionReference());
        dto.setNotes(payment.getNotes());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }
}


package com.svims.controller;

import com.svims.dto.PaymentDTO;
import com.svims.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Payment Controller
 * REST API for Payment operations
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management APIs")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Retrieves all payment records based on the authenticated user's role.
     * - ADMIN and FINANCE: Returns all payments in the system
     * - USER: Returns only payments for invoices created by the current user
     * - MANAGER: No access to payment records
     * 
     * @return ResponseEntity containing list of PaymentDTO objects filtered by user role
     */
    @GetMapping
    @Operation(summary = "Get all payments", description = "Retrieve list of payments based on user role")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'USER')")
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * Retrieves detailed information about a specific payment by its ID.
     * Access is controlled based on user role:
     * - ADMIN and FINANCE: Can access any payment
     * - USER: Can only access payments for invoices they created
     * - MANAGER: No access to payments
     * 
     * @param id The unique identifier of the payment to retrieve
     * @return ResponseEntity containing PaymentDTO with complete payment details
     * @throws RuntimeException if payment not found or access denied based on role
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID", description = "Retrieve payment details by ID (role-based access)")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'USER')")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    /**
     * Retrieves all payment records associated with a specific invoice.
     * This is useful for viewing payment history and tracking partial payments.
     * Access is controlled based on user role:
     * - ADMIN and FINANCE: Can access payments for any invoice
     * - USER: Can only access payments for invoices they created
     * 
     * @param invoiceId The unique identifier of the invoice
     * @return ResponseEntity containing list of PaymentDTO objects for the invoice
     * @throws RuntimeException if invoice not found or access denied based on role
     */
    @GetMapping("/invoice/{invoiceId}")
    @Operation(summary = "Get payments by invoice", description = "Retrieve all payments for an invoice (role-based access)")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE', 'USER')")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByInvoiceId(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(paymentService.getPaymentsByInvoiceId(invoiceId));
    }

    /**
     * Records a new payment (partial or full) for an invoice.
     * This method:
     * - Validates that payment amount doesn't exceed remaining invoice balance
     * - Updates invoice status to PAID if fully paid, or PARTIALLY_PAID if partial
     * - Records payment method, transaction reference, and notes
     * - Creates audit log entry for tracking
     * 
     * Only ADMIN and FINANCE roles can create payments.
     * 
     * @param paymentDTO The payment data transfer object containing amount, invoice ID, payment details
     * @param authentication Spring Security authentication object to get current user
     * @param request HTTP servlet request for capturing IP address in audit log
     * @return ResponseEntity with created PaymentDTO and HTTP 201 status
     * @throws RuntimeException if invoice not found, payment amount exceeds remaining balance, or validation fails
     */
    @PostMapping
    @Operation(summary = "Create payment", description = "Record a payment (partial or full) for an invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE')")
    public ResponseEntity<PaymentDTO> createPayment(@Valid @RequestBody PaymentDTO paymentDTO,
                                                    Authentication authentication,
                                                    HttpServletRequest request) {
        String username = authentication.getName();
        return new ResponseEntity<>(paymentService.createPayment(paymentDTO, username, request), HttpStatus.CREATED);
    }

    /**
     * Permanently deletes a payment record from the system.
     * This operation:
     * - Only ADMIN role can delete payments
     * - Recalculates invoice status based on remaining payments
     * - Sets invoice status to APPROVED if no payments remain, or PARTIALLY_PAID if partial payments exist
     * - Creates audit log entry before deletion
     * - Returns HTTP 204 No Content on success
     * 
     * @param id The unique identifier of the payment to delete
     * @param authentication Spring Security authentication object
     * @param request HTTP servlet request for audit logging
     * @return ResponseEntity with HTTP 204 status
     * @throws RuntimeException if payment not found or access denied
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete payment", description = "Delete a payment record")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id, 
                                               Authentication authentication,
                                               HttpServletRequest request) {
        String username = authentication.getName();
        paymentService.deletePayment(id, username, request);
        return ResponseEntity.noContent().build();
    }
}


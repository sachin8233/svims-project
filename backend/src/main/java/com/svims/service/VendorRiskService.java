package com.svims.service;

import com.svims.entity.Invoice;
import com.svims.entity.Payment;
import com.svims.entity.Vendor;
import com.svims.repository.InvoiceRepository;
import com.svims.repository.PaymentRepository;
import com.svims.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for calculating vendor risk scores
 * Risk factors: late payments, overdue invoices, disputes
 */
@Service
@RequiredArgsConstructor
public class VendorRiskService {

    private final VendorRepository vendorRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Calculate risk score for a vendor (0-100 scale)
     * Higher score = higher risk
     */
    public Double calculateRiskScore(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        List<Invoice> invoices = invoiceRepository.findByVendor(vendor);
        if (invoices.isEmpty()) {
            return 0.0;
        }

        double riskScore = 0.0;

        // Factor 1: Overdue invoices (40 points max)
        long overdueCount = invoices.stream()
                .filter(i -> i.getIsOverdue() != null && i.getIsOverdue())
                .count();
        double overdueScore = Math.min(40.0, (overdueCount * 10.0));
        riskScore += overdueScore;

        // Factor 2: Late payments (30 points max)
        double latePaymentScore = calculateLatePaymentScore(vendorId, invoices);
        riskScore += latePaymentScore;

        // Factor 3: Payment ratio (20 points max)
        double paymentRatio = calculatePaymentRatio(invoices);
        double paymentRatioScore = (1.0 - paymentRatio) * 20.0;
        riskScore += paymentRatioScore;

        // Factor 4: Escalated invoices (10 points max)
        long escalatedCount = invoices.stream()
                .filter(i -> i.getEscalationLevel() != null && i.getEscalationLevel() > 0)
                .count();
        double escalatedScore = Math.min(10.0, escalatedCount * 5.0);
        riskScore += escalatedScore;

        return Math.min(100.0, riskScore);
    }

    private double calculateLatePaymentScore(Long vendorId, List<Invoice> invoices) {
        double latePaymentScore = 0.0;
        int latePaymentCount = 0;

        for (Invoice invoice : invoices) {
            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
                List<Payment> payments = paymentRepository.findByInvoice(invoice);
                if (!payments.isEmpty()) {
                    Payment lastPayment = payments.get(payments.size() - 1);
                    if (lastPayment.getPaymentDate().toLocalDate().isAfter(invoice.getDueDate())) {
                        latePaymentCount++;
                    }
                }
            }
        }

        latePaymentScore = Math.min(30.0, latePaymentCount * 5.0);
        return latePaymentScore;
    }

    private double calculatePaymentRatio(List<Invoice> invoices) {
        BigDecimal totalInvoiceAmount = invoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalInvoiceAmount.compareTo(BigDecimal.ZERO) == 0) {
            return 1.0;
        }

        BigDecimal totalPaidAmount = BigDecimal.ZERO;
        for (Invoice invoice : invoices) {
            BigDecimal paid = paymentRepository.getTotalPaidAmount(invoice);
            totalPaidAmount = totalPaidAmount.add(paid);
        }

        return totalPaidAmount.divide(totalInvoiceAmount, 2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}


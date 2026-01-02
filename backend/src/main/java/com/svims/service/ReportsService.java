package com.svims.service;

import com.svims.dto.ReportsDTO;
import com.svims.entity.Invoice;
import com.svims.entity.Payment;
import com.svims.entity.Vendor;
import com.svims.repository.InvoiceRepository;
import com.svims.repository.PaymentRepository;
import com.svims.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Reports and Analytics
 */
@Service
@RequiredArgsConstructor
public class ReportsService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final VendorRepository vendorRepository;

    public ReportsDTO getDashboardReports() {
        ReportsDTO reports = new ReportsDTO();
        
        List<Invoice> allInvoices = invoiceRepository.findAll();
        List<Payment> allPayments = paymentRepository.findAll();
        List<Vendor> allVendors = vendorRepository.findAll();
        
        // Invoice Statistics
        reports.setInvoiceStats(calculateInvoiceStats(allInvoices));
        
        // Financial Summary
        reports.setFinancialSummary(calculateFinancialSummary(allInvoices, allPayments));
        
        // Vendor Statistics
        reports.setVendorStats(calculateVendorStats(allVendors));
        
        // Status Breakdown
        reports.setStatusBreakdown(calculateStatusBreakdown(allInvoices));
        
        // Monthly Trends (last 6 months)
        reports.setMonthlyTrends(calculateMonthlyTrends(allInvoices, allPayments));
        
        // Top Vendors
        reports.setTopVendors(calculateTopVendors(allInvoices, allVendors));
        
        return reports;
    }

    private ReportsDTO.InvoiceStats calculateInvoiceStats(List<Invoice> invoices) {
        ReportsDTO.InvoiceStats stats = new ReportsDTO.InvoiceStats();
        
        stats.setTotalInvoices((long) invoices.size());
        stats.setPendingInvoices((long) invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PENDING).count());
        stats.setApprovedInvoices((long) invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.APPROVED).count());
        stats.setPaidInvoices((long) invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PAID).count());
        stats.setOverdueInvoices((long) invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.OVERDUE || 
                            Boolean.TRUE.equals(i.getIsOverdue())).count());
        stats.setRejectedInvoices((long) invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.REJECTED).count());
        
        stats.setTotalInvoiceAmount(invoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        stats.setPendingAmount(invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PENDING)
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        stats.setApprovedAmount(invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.APPROVED)
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        stats.setPaidAmount(invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.PAID)
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        stats.setOverdueAmount(invoices.stream()
                .filter(i -> i.getStatus() == Invoice.InvoiceStatus.OVERDUE || 
                            Boolean.TRUE.equals(i.getIsOverdue()))
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        return stats;
    }

    private ReportsDTO.FinancialSummary calculateFinancialSummary(List<Invoice> invoices, List<Payment> payments) {
        ReportsDTO.FinancialSummary summary = new ReportsDTO.FinancialSummary();
        
        summary.setTotalInvoiceAmount(invoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        summary.setTotalPaidAmount(payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        summary.setTotalOutstandingAmount(summary.getTotalInvoiceAmount()
                .subtract(summary.getTotalPaidAmount()));
        
        summary.setTotalCgstAmount(invoices.stream()
                .map(i -> i.getCgstAmount() != null ? i.getCgstAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        summary.setTotalSgstAmount(invoices.stream()
                .map(i -> i.getSgstAmount() != null ? i.getSgstAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        summary.setTotalIgstAmount(invoices.stream()
                .map(i -> i.getIgstAmount() != null ? i.getIgstAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        summary.setTotalGstAmount(summary.getTotalCgstAmount()
                .add(summary.getTotalSgstAmount())
                .add(summary.getTotalIgstAmount()));
        
        return summary;
    }

    private ReportsDTO.VendorStats calculateVendorStats(List<Vendor> vendors) {
        ReportsDTO.VendorStats stats = new ReportsDTO.VendorStats();
        
        stats.setTotalVendors((long) vendors.size());
        stats.setActiveVendors((long) vendors.stream()
                .filter(v -> v.getStatus() == Vendor.VendorStatus.ACTIVE).count());
        stats.setHighRiskVendors((long) vendors.stream()
                .filter(v -> v.getRiskScore() != null && v.getRiskScore() > 50.0).count());
        
        OptionalDouble avgRisk = vendors.stream()
                .filter(v -> v.getRiskScore() != null)
                .mapToDouble(Vendor::getRiskScore)
                .average();
        
        stats.setAverageRiskScore(avgRisk.isPresent() ? avgRisk.getAsDouble() : 0.0);
        
        return stats;
    }

    private Map<String, Long> calculateStatusBreakdown(List<Invoice> invoices) {
        Map<String, Long> breakdown = new HashMap<>();
        for (Invoice.InvoiceStatus status : Invoice.InvoiceStatus.values()) {
            breakdown.put(status.name(), 
                invoices.stream().filter(i -> i.getStatus() == status).count());
        }
        return breakdown;
    }

    private List<ReportsDTO.MonthlyData> calculateMonthlyTrends(List<Invoice> invoices, List<Payment> payments) {
        List<ReportsDTO.MonthlyData> trends = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        
        for (int i = 5; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            
            List<Invoice> monthInvoices = invoices.stream()
                    .filter(inv -> {
                        LocalDate invDate = inv.getInvoiceDate();
                        return invDate != null && 
                               !invDate.isBefore(monthStart) && 
                               !invDate.isAfter(monthEnd);
                    })
                    .collect(Collectors.toList());
            
            List<Payment> monthPayments = payments.stream()
                    .filter(p -> {
                        LocalDate payDate = p.getPaymentDate() != null ? 
                                p.getPaymentDate().toLocalDate() : null;
                        return payDate != null && 
                               !payDate.isBefore(monthStart) && 
                               !payDate.isAfter(monthEnd);
                    })
                    .collect(Collectors.toList());
            
            ReportsDTO.MonthlyData data = new ReportsDTO.MonthlyData();
            data.setMonth(monthStart.format(formatter));
            data.setInvoiceCount((long) monthInvoices.size());
            data.setInvoiceAmount(monthInvoices.stream()
                    .map(Invoice::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            data.setPaidAmount(monthPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            
            trends.add(data);
        }
        
        return trends;
    }

    private List<ReportsDTO.VendorSummary> calculateTopVendors(List<Invoice> invoices, List<Vendor> vendors) {
        Map<Long, ReportsDTO.VendorSummary> vendorMap = new HashMap<>();
        
        // Initialize vendor summaries
        for (Vendor vendor : vendors) {
            ReportsDTO.VendorSummary summary = new ReportsDTO.VendorSummary();
            summary.setVendorId(vendor.getId());
            summary.setVendorName(vendor.getName());
            summary.setInvoiceCount(0L);
            summary.setTotalAmount(BigDecimal.ZERO);
            summary.setPaidAmount(BigDecimal.ZERO);
            summary.setOutstandingAmount(BigDecimal.ZERO);
            summary.setRiskScore(vendor.getRiskScore());
            vendorMap.put(vendor.getId(), summary);
        }
        
        // Aggregate invoice data by vendor
        for (Invoice invoice : invoices) {
            Long vendorId = invoice.getVendor().getId();
            ReportsDTO.VendorSummary summary = vendorMap.get(vendorId);
            
            if (summary != null) {
                summary.setInvoiceCount(summary.getInvoiceCount() + 1);
                summary.setTotalAmount(summary.getTotalAmount().add(invoice.getTotalAmount()));
                
                // Calculate paid amount
                BigDecimal paid = paymentRepository.getTotalPaidAmount(invoice);
                summary.setPaidAmount(summary.getPaidAmount().add(paid));
            }
        }
        
        // Calculate outstanding amounts
        for (ReportsDTO.VendorSummary summary : vendorMap.values()) {
            summary.setOutstandingAmount(summary.getTotalAmount().subtract(summary.getPaidAmount()));
        }
        
        // Sort by total amount descending and return top 10
        return vendorMap.values().stream()
                .sorted((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()))
                .limit(10)
                .collect(Collectors.toList());
    }
}


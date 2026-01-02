package com.svims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Reports
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportsDTO {
    
    // Invoice Statistics
    private InvoiceStats invoiceStats;
    
    // Financial Summary
    private FinancialSummary financialSummary;
    
    // Vendor Statistics
    private VendorStats vendorStats;
    
    // Status Breakdown
    private Map<String, Long> statusBreakdown;
    
    // Monthly Trends
    private List<MonthlyData> monthlyTrends;
    
    // Top Vendors
    private List<VendorSummary> topVendors;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvoiceStats {
        private Long totalInvoices;
        private Long pendingInvoices;
        private Long approvedInvoices;
        private Long paidInvoices;
        private Long overdueInvoices;
        private Long rejectedInvoices;
        private BigDecimal totalInvoiceAmount;
        private BigDecimal pendingAmount;
        private BigDecimal approvedAmount;
        private BigDecimal paidAmount;
        private BigDecimal overdueAmount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialSummary {
        private BigDecimal totalInvoiceAmount;
        private BigDecimal totalPaidAmount;
        private BigDecimal totalOutstandingAmount;
        private BigDecimal totalCgstAmount;
        private BigDecimal totalSgstAmount;
        private BigDecimal totalIgstAmount;
        private BigDecimal totalGstAmount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorStats {
        private Long totalVendors;
        private Long activeVendors;
        private Long highRiskVendors;
        private Double averageRiskScore;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyData {
        private String month;
        private Long invoiceCount;
        private BigDecimal invoiceAmount;
        private BigDecimal paidAmount;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorSummary {
        private Long vendorId;
        private String vendorName;
        private Long invoiceCount;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal outstandingAmount;
        private Double riskScore;
    }
}



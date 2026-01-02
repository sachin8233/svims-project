package com.svims.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Service for GST calculations
 * Handles CGST, SGST, and IGST calculations based on vendor location
 */
@Service
public class GstService {

    private static final BigDecimal GST_RATE = new BigDecimal("18.00"); // 18% GST
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    /**
     * Calculate GST components based on vendor and invoice location
     * For same state: CGST + SGST (9% each)
     * For different state: IGST (18%)
     * 
     * @param amount Base amount
     * @param vendorState State code of vendor
     * @param invoiceState State code where invoice is generated
     * @return GSTCalculationResult containing CGST, SGST, IGST, and total
     */
    public GSTCalculationResult calculateGST(BigDecimal amount, String vendorState, String invoiceState) {
        boolean isSameState = vendorState != null && vendorState.equals(invoiceState);
        
        BigDecimal cgstAmount = BigDecimal.ZERO;
        BigDecimal sgstAmount = BigDecimal.ZERO;
        BigDecimal igstAmount = BigDecimal.ZERO;
        
        if (isSameState) {
            // Same state: CGST + SGST (9% each)
            BigDecimal halfRate = GST_RATE.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
            cgstAmount = amount.multiply(halfRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            sgstAmount = cgstAmount;
        } else {
            // Different state: IGST (18%)
            igstAmount = amount.multiply(GST_RATE).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        }
        
        BigDecimal totalAmount = amount.add(cgstAmount).add(sgstAmount).add(igstAmount);
        
        return new GSTCalculationResult(cgstAmount, sgstAmount, igstAmount, totalAmount);
    }

    /**
     * Result class for GST calculation
     */
    public static class GSTCalculationResult {
        private final BigDecimal cgstAmount;
        private final BigDecimal sgstAmount;
        private final BigDecimal igstAmount;
        private final BigDecimal totalAmount;

        public GSTCalculationResult(BigDecimal cgstAmount, BigDecimal sgstAmount, 
                                   BigDecimal igstAmount, BigDecimal totalAmount) {
            this.cgstAmount = cgstAmount;
            this.sgstAmount = sgstAmount;
            this.igstAmount = igstAmount;
            this.totalAmount = totalAmount;
        }

        public BigDecimal getCgstAmount() { return cgstAmount; }
        public BigDecimal getSgstAmount() { return sgstAmount; }
        public BigDecimal getIgstAmount() { return igstAmount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
    }
}


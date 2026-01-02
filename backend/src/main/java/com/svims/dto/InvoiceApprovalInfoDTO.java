package com.svims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Invoice Approval Information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceApprovalInfoDTO {
    private Integer currentApprovalLevel;
    private Integer requiredApprovalLevels;
    private Integer remainingApprovals;
    private Boolean isFullyApproved;
    private String approvalRuleRange; // e.g., "₹0 - ₹50,000"
}



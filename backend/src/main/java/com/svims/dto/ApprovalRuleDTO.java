package com.svims.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for ApprovalRule
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRuleDTO {
    private Long id;
    
    @NotNull(message = "Minimum amount is required")
    @DecimalMin(value = "0.0", message = "Minimum amount must be >= 0")
    private BigDecimal minAmount;
    
    @NotNull(message = "Maximum amount is required")
    @DecimalMin(value = "0.01", message = "Maximum amount must be > 0")
    private BigDecimal maxAmount;
    
    @NotNull(message = "Approval levels required")
    @Min(value = 1, message = "At least 1 approval level required")
    private Integer approvalLevels;
    
    private String requiredRoles; // Comma-separated: ROLE_MANAGER,ROLE_FINANCE,ROLE_ADMIN
    
    private Boolean isActive = true;
    
    private Integer priority = 0;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}



package com.svims.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ApprovalRule Entity
 * Defines multi-level approval rules based on invoice amount ranges
 */
@Entity
@Table(name = "approval_rules", indexes = {
    @Index(name = "idx_rule_amount_range", columnList = "min_amount, max_amount")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Minimum amount is required")
    @DecimalMin(value = "0.0", message = "Minimum amount must be >= 0")
    @Column(name = "min_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    @NotNull(message = "Maximum amount is required")
    @DecimalMin(value = "0.01", message = "Maximum amount must be > 0")
    @Column(name = "max_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal maxAmount;

    @NotNull(message = "Approval levels required")
    @Min(value = 1, message = "At least 1 approval level required")
    @Column(name = "approval_levels", nullable = false)
    private Integer approvalLevels;

    @Column(name = "required_roles", length = 200)
    private String requiredRoles; // Comma-separated: MANAGER,FINANCE,ADMIN

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "priority")
    private Integer priority = 0; // Lower number = higher priority

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


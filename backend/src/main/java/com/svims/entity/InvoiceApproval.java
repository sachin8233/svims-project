package com.svims.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * InvoiceApproval Entity
 * Tracks approval history for invoices at different levels
 */
@Entity
@Table(name = "invoice_approvals", indexes = {
    @Index(name = "idx_approval_invoice", columnList = "invoice_id"),
    @Index(name = "idx_approval_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "approval_level", nullable = false)
    private Integer approvalLevel;

    @Column(name = "approved_by", nullable = false, length = 100)
    private String approvedBy; // Username

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status;

    @Column(name = "comments", length = 500)
    private String comments;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
}


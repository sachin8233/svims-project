package com.svims.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Invoice Entity
 * Represents an invoice from a vendor
 */
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_date", columnList = "invoice_date"),
    @Index(name = "idx_invoice_status", columnList = "status"),
    @Index(name = "idx_invoice_vendor", columnList = "vendor_id"),
    @Index(name = "idx_invoice_due_date", columnList = "due_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Vendor is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @NotNull(message = "Invoice amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "cgst_amount", precision = 15, scale = 2)
    private BigDecimal cgstAmount;

    @Column(name = "sgst_amount", precision = 15, scale = 2)
    private BigDecimal sgstAmount;

    @Column(name = "igst_amount", precision = 15, scale = 2)
    private BigDecimal igstAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @NotNull(message = "Invoice date is required")
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @NotNull(message = "Due date is required")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "current_approval_level")
    private Integer currentApprovalLevel = 0;

    @Column(name = "is_overdue")
    private Boolean isOverdue = false;

    @Column(name = "escalation_level")
    private Integer escalationLevel = 0;

    @Column(name = "invoice_number", unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvoiceApproval> approvals;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("itemOrder ASC")
    private List<InvoiceItem> items;

    public enum InvoiceStatus {
        PENDING, APPROVED, REJECTED, PAID, PARTIALLY_PAID, OVERDUE, ESCALATED
    }
}


package com.svims.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * InvoiceItem Entity
 * Represents a line item in an invoice
 */
@Entity
@Table(name = "invoice_items", indexes = {
    @Index(name = "idx_item_invoice", columnList = "invoice_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Invoice is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @NotBlank(message = "Item description is required")
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; // Calculated: quantity * unitPrice

    @Column(name = "item_order")
    private Integer itemOrder; // Order of item in invoice

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Calculate amount before persisting
    @PrePersist
    @PreUpdate
    private void calculateAmount() {
        if (quantity != null && unitPrice != null) {
            this.amount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}



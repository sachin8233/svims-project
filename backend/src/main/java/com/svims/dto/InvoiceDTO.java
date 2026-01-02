package com.svims.dto;

import com.svims.entity.Invoice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Invoice
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    private Long id;
    
    @NotNull(message = "Vendor ID is required")
    private Long vendorId;
    
    private String vendorName;
    
    // Amount is now calculated from items, but kept for backward compatibility
    private BigDecimal amount;
    
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal igstAmount;
    private BigDecimal totalAmount;
    
    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;
    
    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
    
    @NotEmpty(message = "At least one invoice item is required")
    @Valid
    private List<InvoiceItemDTO> items;
    
    private Invoice.InvoiceStatus status;
    private Integer currentApprovalLevel;
    private Boolean isOverdue;
    private Integer escalationLevel;
    private String invoiceNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


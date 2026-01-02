package com.svims.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vendor Entity
 * Represents a vendor/supplier in the system
 */
@Entity
@Table(name = "vendors", indexes = {
    @Index(name = "idx_vendor_email", columnList = "email"),
    @Index(name = "idx_vendor_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Vendor name is required")
    @Column(nullable = false, length = 100)
    private String name;

    @Pattern(regexp = "^$|^[0-9]+$", 
             message = "GSTIN must contain only numbers")
    @Column(unique = true, length = 50)
    private String gstin;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VendorStatus status = VendorStatus.ACTIVE;

    @Column(name = "risk_score")
    private Double riskScore = 0.0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Invoice> invoices;

    public enum VendorStatus {
        ACTIVE, INACTIVE, SUSPENDED
    }
}


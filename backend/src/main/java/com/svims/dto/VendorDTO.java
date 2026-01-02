package com.svims.dto;

import com.svims.entity.Vendor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Vendor
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VendorDTO {
    private Long id;
    
    @NotBlank(message = "Vendor name is required")
    private String name;
    
    @Pattern(regexp = "^$|^[0-9]+$", 
             message = "GSTIN must contain only numbers")
    private String gstin;
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;
    
    private Vendor.VendorStatus status;
    private Double riskScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


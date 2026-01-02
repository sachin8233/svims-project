package com.svims.service;

import com.svims.dto.VendorDTO;
import com.svims.entity.Vendor;
import com.svims.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Vendor operations
 */
@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final VendorRiskService vendorRiskService;
    private final AuditService auditService;

    /**
     * Retrieves all vendors registered in the system.
     * 
     * Returns complete vendor information including:
     * - Vendor name, email, and GSTIN
     * - Vendor status (ACTIVE, INACTIVE, SUSPENDED)
     * - Risk score for vendor assessment
     * - Creation and update timestamps
     * 
     * @return List of all VendorDTO objects in the system
     */
    public List<VendorDTO> getAllVendors() {
        return vendorRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves detailed information about a specific vendor by its ID.
     * 
     * @param id The unique identifier of the vendor to retrieve
     * @return VendorDTO containing complete vendor details
     * @throws RuntimeException if vendor not found
     */
    public VendorDTO getVendorById(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + id));
        return convertToDTO(vendor);
    }

    /**
     * Creates a new vendor in the system.
     * 
     * This method:
     * - Validates that vendor email is unique (prevents duplicate vendors)
     * - Validates that GSTIN is unique if provided
     * - Sets default status to ACTIVE if not specified
     * - Initializes risk score to 0.0
     * - Creates audit log entry for tracking
     * 
     * @param vendorDTO The vendor data transfer object containing name, email, GSTIN, status
     * @param userName The username of the user creating the vendor (for audit trail)
     * @param request HTTP servlet request for capturing IP address in audit log
     * @return VendorDTO containing the created vendor with assigned ID
     * @throws RuntimeException if vendor with same email or GSTIN already exists
     */
    @Transactional
    public VendorDTO createVendor(VendorDTO vendorDTO, String userName, HttpServletRequest request) {
        if (vendorRepository.findByEmail(vendorDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Vendor with email already exists: " + vendorDTO.getEmail());
        }
        if (vendorDTO.getGstin() != null && 
            vendorRepository.findByGstin(vendorDTO.getGstin()).isPresent()) {
            throw new RuntimeException("Vendor with GSTIN already exists: " + vendorDTO.getGstin());
        }

        Vendor vendor = convertToEntity(vendorDTO);
        vendor = vendorRepository.save(vendor);
        
        // Log audit
        auditService.logAction(userName, "CREATE", "VENDOR", vendor.getId(),
                null, vendorDTO, "Vendor created: " + vendor.getName(), request);
        
        return convertToDTO(vendor);
    }

    /**
     * Updates an existing vendor's information.
     * 
     * This method:
     * - Updates vendor name, email, GSTIN, and status
     * - Preserves existing risk score unless explicitly changed in DTO
     * - Creates audit log entry with old and new values for tracking changes
     * 
     * @param id The unique identifier of the vendor to update
     * @param vendorDTO The updated vendor data
     * @param userName The username of the user updating the vendor (for audit trail)
     * @param request HTTP servlet request for capturing IP address in audit log
     * @return VendorDTO containing the updated vendor information
     * @throws RuntimeException if vendor not found
     */
    @Transactional
    public VendorDTO updateVendor(Long id, VendorDTO vendorDTO, String userName, HttpServletRequest request) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + id));

        VendorDTO oldDTO = convertToDTO(vendor);

        vendor.setName(vendorDTO.getName());
        vendor.setEmail(vendorDTO.getEmail());
        vendor.setGstin(vendorDTO.getGstin());
        if (vendorDTO.getStatus() != null) {
            vendor.setStatus(vendorDTO.getStatus());
        }

        vendor = vendorRepository.save(vendor);
        
        // Log audit
        auditService.logAction(userName, "UPDATE", "VENDOR", vendor.getId(),
                oldDTO, convertToDTO(vendor), "Vendor updated: " + vendor.getName(), request);
        
        return convertToDTO(vendor);
    }

    /**
     * Permanently deletes a vendor from the system.
     * 
     * This method:
     * - Removes the vendor record completely from the database
     * - Creates audit log entry with vendor details before deletion
     * 
     * Note: Consider changing vendor status to INACTIVE instead of deletion
     * if the vendor has associated invoices, to maintain data integrity.
     * 
     * @param id The unique identifier of the vendor to delete
     * @param userName The username of the user deleting the vendor (for audit trail)
     * @param request HTTP servlet request for capturing IP address in audit log
     * @throws RuntimeException if vendor not found
     */
    @Transactional
    public void deleteVendor(Long id, String userName, HttpServletRequest request) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + id));
        
        VendorDTO oldDTO = convertToDTO(vendor);
        vendorRepository.delete(vendor);
        
        // Log audit
        auditService.logAction(userName, "DELETE", "VENDOR", id,
                oldDTO, null, "Vendor deleted: " + oldDTO.getName(), request);
    }

    /**
     * Retrieves vendors with risk scores above a specified threshold.
     * 
     * This method is useful for:
     * - Identifying vendors that may require additional scrutiny
     * - Monitoring vendors with high risk scores
     * - Generating risk assessment reports
     * 
     * Risk scores are calculated based on various factors including:
     * - Payment history and patterns
     * - Invoice patterns and amounts
     * - Vendor performance metrics
     * 
     * @param threshold The minimum risk score threshold (default: 50.0)
     * @return List of VendorDTO objects with risk scores above the threshold
     */
    public List<VendorDTO> getHighRiskVendors(Double threshold) {
        return vendorRepository.findHighRiskVendors(threshold).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Recalculates and updates the risk score for a specific vendor.
     * 
     * This method:
     * - Calculates the risk score using VendorRiskService based on various factors
     * - Updates the vendor's risk score in the database
     * - Should be called periodically or when vendor-related data changes
     * 
     * Risk score calculation considers:
     * - Payment history and delays
     * - Invoice patterns
     * - Vendor performance metrics
     * 
     * @param vendorId The unique identifier of the vendor
     * @throws RuntimeException if vendor not found
     */
    @Transactional
    public void updateVendorRiskScore(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + vendorId));
        Double riskScore = vendorRiskService.calculateRiskScore(vendorId);
        vendor.setRiskScore(riskScore);
        vendorRepository.save(vendor);
    }

    private VendorDTO convertToDTO(Vendor vendor) {
        VendorDTO dto = new VendorDTO();
        dto.setId(vendor.getId());
        dto.setName(vendor.getName());
        dto.setGstin(vendor.getGstin());
        dto.setEmail(vendor.getEmail());
        dto.setStatus(vendor.getStatus());
        dto.setRiskScore(vendor.getRiskScore());
        dto.setCreatedAt(vendor.getCreatedAt());
        dto.setUpdatedAt(vendor.getUpdatedAt());
        return dto;
    }

    private Vendor convertToEntity(VendorDTO dto) {
        Vendor vendor = new Vendor();
        vendor.setName(dto.getName());
        // Set GSTIN only if it's not null and not empty
        if (dto.getGstin() != null && !dto.getGstin().trim().isEmpty()) {
            vendor.setGstin(dto.getGstin().trim());
        } else {
            vendor.setGstin(null);
        }
        vendor.setEmail(dto.getEmail());
        vendor.setStatus(dto.getStatus() != null ? dto.getStatus() : Vendor.VendorStatus.ACTIVE);
        vendor.setRiskScore(dto.getRiskScore() != null ? dto.getRiskScore() : 0.0);
        return vendor;
    }
}


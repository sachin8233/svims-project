package com.svims.controller;

import com.svims.dto.VendorDTO;
import com.svims.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vendor Controller
 * REST API for Vendor CRUD operations
 */
@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Vendor management APIs")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class VendorController {

    private final VendorService vendorService;

    /**
     * Retrieves all vendors registered in the system.
     * Returns complete vendor information including name, email, GSTIN, status, and risk score.
     * 
     * ADMIN, MANAGER, and FINANCE roles can view all vendors.
     * 
     * @return ResponseEntity containing list of all VendorDTO objects
     */
    @GetMapping
    @Operation(summary = "Get all vendors", description = "Retrieve list of all vendors")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<List<VendorDTO>> getAllVendors() {
        return ResponseEntity.ok(vendorService.getAllVendors());
    }

    /**
     * Retrieves detailed information about a specific vendor by its ID.
     * Returns vendor details including contact information, GSTIN, status, and risk assessment.
     * 
     * @param id The unique identifier of the vendor to retrieve
     * @return ResponseEntity containing VendorDTO with complete vendor details
     * @throws RuntimeException if vendor not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get vendor by ID", description = "Retrieve vendor details by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<VendorDTO> getVendorById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }

    /**
     * Creates a new vendor in the system.
     * This method:
     * - Validates that vendor email and GSTIN are unique
     * - Sets default status to ACTIVE if not specified
     * - Initializes risk score to 0.0
     * - Creates audit log entry for tracking
     * 
     * Only ADMIN and MANAGER roles can create vendors.
     * 
     * @param vendorDTO The vendor data transfer object containing name, email, GSTIN, status
     * @param authentication Spring Security authentication object to get current user
     * @param request HTTP servlet request for capturing IP address in audit log
     * @return ResponseEntity with created VendorDTO and HTTP 201 status, or error response if validation fails
     */
    @PostMapping
    @Operation(summary = "Create vendor", description = "Create a new vendor")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> createVendor(@Valid @RequestBody VendorDTO vendorDTO,
                                         Authentication authentication,
                                         HttpServletRequest request) {
        try {
            String username = authentication.getName();
            return new ResponseEntity<>(vendorService.createVendor(vendorDTO, username, request), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create vendor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Updates an existing vendor's information.
     * This method:
     * - Updates vendor name, email, GSTIN, and status
     * - Preserves existing risk score unless explicitly changed
     * - Creates audit log entry with old and new values
     * 
     * Only ADMIN and MANAGER roles can update vendors.
     * 
     * @param id The unique identifier of the vendor to update
     * @param vendorDTO The updated vendor data
     * @param authentication Spring Security authentication object
     * @param request HTTP servlet request for audit logging
     * @return ResponseEntity with updated VendorDTO
     * @throws RuntimeException if vendor not found
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update vendor", description = "Update existing vendor details")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<VendorDTO> updateVendor(@PathVariable Long id, 
                                                  @Valid @RequestBody VendorDTO vendorDTO,
                                                  Authentication authentication,
                                                  HttpServletRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(vendorService.updateVendor(id, vendorDTO, username, request));
    }

    /**
     * Permanently deletes a vendor from the system.
     * This operation:
     * - Only ADMIN role can delete vendors
     * - Removes the vendor record completely
     * - Creates audit log entry before deletion
     * - Returns HTTP 204 No Content on success
     * 
     * Note: Consider vendor status change to INACTIVE instead of deletion if vendor has associated invoices.
     * 
     * @param id The unique identifier of the vendor to delete
     * @param authentication Spring Security authentication object
     * @param request HTTP servlet request for audit logging
     * @return ResponseEntity with HTTP 204 status
     * @throws RuntimeException if vendor not found or access denied
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete vendor", description = "Delete a vendor by ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long id,
                                             Authentication authentication,
                                             HttpServletRequest request) {
        String username = authentication.getName();
        vendorService.deleteVendor(id, username, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves vendors with risk scores above a specified threshold.
     * This is useful for identifying vendors that may require additional scrutiny
     * or monitoring based on their risk assessment score.
     * 
     * Risk scores are calculated based on various factors including payment history,
     * invoice patterns, and vendor performance metrics.
     * 
     * @param threshold The minimum risk score threshold (default: 50.0)
     * @return ResponseEntity containing list of VendorDTO objects with risk scores above threshold
     */
    @GetMapping("/high-risk")
    @Operation(summary = "Get high-risk vendors", description = "Retrieve vendors with risk score above threshold")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<VendorDTO>> getHighRiskVendors(
            @RequestParam(defaultValue = "50.0") Double threshold) {
        return ResponseEntity.ok(vendorService.getHighRiskVendors(threshold));
    }
}


package com.svims.controller;

import com.svims.dto.InvoiceDTO;
import com.svims.dto.InvoiceApprovalInfoDTO;
import com.svims.service.InvoiceService;
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

import java.util.List;

/**
 * Invoice Controller
 * REST API for Invoice CRUD and approval operations
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management APIs")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Retrieves all invoices based on the authenticated user's role.
     * - ADMIN: Returns all invoices in the system
     * - MANAGER: Returns only pending invoices awaiting approval
     * - FINANCE: Returns only approved invoices ready for payment processing
     * - USER: Returns only invoices created by the current user
     * 
     * @return ResponseEntity containing list of InvoiceDTO objects filtered by user role
     */
    @GetMapping
    @Operation(summary = "Get all invoices", description = "Retrieve list of invoices based on user role")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'USER')")
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    /**
     * Retrieves detailed information about a specific invoice by its ID.
     * Access is controlled based on user role:
     * - ADMIN: Can access any invoice
     * - MANAGER: Can only access pending invoices
     * - FINANCE: Can only access approved invoices
     * - USER: Can only access invoices they created
     * 
     * @param id The unique identifier of the invoice to retrieve
     * @return ResponseEntity containing InvoiceDTO with complete invoice details
     * @throws RuntimeException if invoice not found or access denied based on role
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get invoice by ID", description = "Retrieve invoice details by ID (role-based access)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'USER')")
    public ResponseEntity<InvoiceDTO> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    /**
     * Creates a new invoice in the system with automatic GST calculation and invoice number generation.
     * This method:
     * - Validates vendor existence and invoice items
     * - Calculates subtotal from invoice items
     * - Automatically calculates CGST, SGST, and IGST based on vendor GSTIN
     * - Generates a unique invoice number (format: INV-YYYYMMDD-XXXX)
     * - Sets invoice status to PENDING for approval workflow
     * - Creates audit log entry for tracking
     * 
     * Only ADMIN and USER roles can create invoices.
     * 
     * @param invoiceDTO The invoice data transfer object containing vendor, items, dates, etc.
     * @param authentication Spring Security authentication object to get current user
     * @param request HTTP servlet request for capturing IP address in audit log
     * @return ResponseEntity with created InvoiceDTO and HTTP 201 status
     * @throws RuntimeException if vendor not found, items missing, or validation fails
     */
    @PostMapping
    @Operation(summary = "Create invoice", description = "Create a new invoice with GST calculation (USER/ADMIN only)")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<InvoiceDTO> createInvoice(@Valid @RequestBody InvoiceDTO invoiceDTO,
                                                    Authentication authentication,
                                                    HttpServletRequest request) {
        String username = authentication.getName();
        return new ResponseEntity<>(invoiceService.createInvoice(invoiceDTO, username, request), HttpStatus.CREATED);
    }

    /**
     * Updates an existing invoice's details including items, dates, and amounts.
     * This method:
     * - Only allows ADMIN role to update invoices
     * - Prevents editing of APPROVED, PAID, or PARTIALLY_PAID invoices
     * - Recalculates GST when invoice items or amounts change
     * - Resets status to PENDING if invoice was previously REJECTED
     * - Creates audit log entry with old and new values
     * 
     * @param id The unique identifier of the invoice to update
     * @param invoiceDTO The updated invoice data
     * @param authentication Spring Security authentication object
     * @param request HTTP servlet request for audit logging
     * @return ResponseEntity with updated InvoiceDTO
     * @throws RuntimeException if invoice not found, access denied, or invoice status prevents editing
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update invoice", description = "Update existing invoice details")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<InvoiceDTO> updateInvoice(@PathVariable Long id,
                                                     @Valid @RequestBody InvoiceDTO invoiceDTO,
                                                     Authentication authentication,
                                                     HttpServletRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(invoiceService.updateInvoice(id, invoiceDTO, username, request));
    }


    /**
     * Approves an invoice at a specific approval level in the multi-level approval workflow.
     * This method:
     * - Validates that the user hasn't already approved this invoice
     * - Ensures the approval level is sequential (currentLevel + 1)
     * - Checks if the level has already been approved by another user
     * - Updates invoice status to APPROVED when all required levels are completed
     * - Creates an approval record and audit log entry
     * 
     * ADMIN, MANAGER, and FINANCE roles can approve invoices.
     * 
     * @param id The unique identifier of the invoice to approve
     * @param level The approval level (must be currentLevel + 1)
     * @param comments Optional comments for the approval decision
     * @param authentication Spring Security authentication object
     * @param request HTTP servlet request for audit logging
     * @return ResponseEntity with updated InvoiceDTO
     * @throws RuntimeException if validation fails, duplicate approval, or invalid level
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve invoice", description = "Approve invoice at specified level")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<InvoiceDTO> approveInvoice(@PathVariable Long id,
                                                     @RequestParam Integer level,
                                                     @RequestParam(required = false) String comments,
                                                     Authentication authentication,
                                                     HttpServletRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(invoiceService.approveInvoice(id, level, username, comments, request));
    }

    /**
     * Rejects an invoice, stopping the approval workflow.
     * This method:
     * - Changes invoice status to REJECTED
     * - Creates a rejection record with comments explaining the rejection
     * - Creates audit log entry for tracking
     * 
     * ADMIN, MANAGER, and FINANCE roles can reject invoices.
     * Rejection comments are required to document the reason.
     * 
     * @param id The unique identifier of the invoice to reject
     * @param comments Required comments explaining the rejection reason
     * @param authentication Spring Security authentication object
     * @param request HTTP servlet request for audit logging
     * @return ResponseEntity with updated InvoiceDTO showing REJECTED status
     * @throws RuntimeException if invoice not found
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject invoice", description = "Reject an invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<InvoiceDTO> rejectInvoice(@PathVariable Long id,
                                                     @RequestParam String comments,
                                                     Authentication authentication,
                                                     HttpServletRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(invoiceService.rejectInvoice(id, username, comments, request));
    }

    /**
     * Retrieves all invoices that have passed their due date and are marked as overdue.
     * Overdue invoices are those where the due date is in the past and the invoice
     * has not been fully paid. This is useful for tracking payment delays and
     * managing collections.
     * 
     * ADMIN, MANAGER, and FINANCE roles can view overdue invoices.
     * 
     * @return ResponseEntity containing list of overdue InvoiceDTO objects
     */
    @GetMapping("/overdue")
    @Operation(summary = "Get overdue invoices", description = "Retrieve all overdue invoices")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<List<InvoiceDTO>> getOverdueInvoices() {
        return ResponseEntity.ok(invoiceService.getOverdueInvoices());
    }

    /**
     * Retrieves detailed approval workflow information for a specific invoice.
     * Returns information about:
     * - Current approval level progress
     * - Required approval levels based on invoice amount
     * - Remaining approvals needed
     * - Whether the invoice is fully approved
     * - Applicable approval rule range
     * 
     * This is useful for displaying approval progress bars and status in the UI.
     * 
     * @param id The unique identifier of the invoice
     * @return ResponseEntity containing InvoiceApprovalInfoDTO with approval details
     * @throws RuntimeException if invoice not found
     */
    @GetMapping("/{id}/approval-info")
    @Operation(summary = "Get invoice approval information", description = "Get approval level details for an invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<InvoiceApprovalInfoDTO> getInvoiceApprovalInfo(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoiceApprovalInfo(id));
    }
}


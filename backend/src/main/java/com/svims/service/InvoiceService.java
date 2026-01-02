package com.svims.service;

import com.svims.dto.InvoiceDTO;
import com.svims.dto.InvoiceApprovalInfoDTO;
import com.svims.dto.InvoiceItemDTO;
import com.svims.entity.*;
import com.svims.repository.*;
import com.svims.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for Invoice operations
 * Handles CRUD, GST calculation, approval workflow, and status management
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final VendorRepository vendorRepository;
    private final PaymentRepository paymentRepository;
    private final ApprovalRuleRepository approvalRuleRepository;
    private final InvoiceApprovalRepository invoiceApprovalRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final GstService gstService;
    private final AuditService auditService;
    private final SecurityUtil securityUtil;

    /**
     * Get all invoices based on user role:
     * - USER: Only their own invoices
     * - MANAGER: Pending invoices for approval
     * - FINANCE: Approved invoices ready for payment
     * - ADMIN: All invoices
     */
    public List<InvoiceDTO> getAllInvoices() {
        String currentUsername = securityUtil.getCurrentUsername();
        List<Invoice> invoices;

        if (securityUtil.isAdmin()) {
            // ADMIN can see all invoices
            invoices = invoiceRepository.findAll();
        } else if (securityUtil.isManager()) {
            // MANAGER can see pending invoices for approval
            invoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING);
        } else if (securityUtil.isFinance()) {
            // FINANCE can see approved invoices ready for payment
            invoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.APPROVED);
        } else {
            // USER can only see their own invoices
            // Filter to include only invoices created by the user (handle null createdBy for old invoices)
            List<Invoice> allInvoices = invoiceRepository.findAll();
            invoices = allInvoices.stream()
                    .filter(inv -> inv.getCreatedBy() != null && inv.getCreatedBy().equals(currentUsername))
                    .collect(Collectors.toList());
        }

        return invoices.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get invoice by ID with role-based access control
     */
    public InvoiceDTO getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
        
        String currentUsername = securityUtil.getCurrentUsername();
        
        // Check access based on role
        if (!securityUtil.isAdmin()) {
            if (securityUtil.isUser()) {
                // USER can only access their own invoices
                if (invoice.getCreatedBy() == null || !invoice.getCreatedBy().equals(currentUsername)) {
                    throw new RuntimeException("Access denied: You can only view your own invoices");
                }
            } else if (securityUtil.isManager()) {
                // MANAGER can only access pending invoices
                if (invoice.getStatus() != Invoice.InvoiceStatus.PENDING) {
                    throw new RuntimeException("Access denied: Managers can only view pending invoices");
                }
            } else if (securityUtil.isFinance()) {
                // FINANCE can only access approved invoices
                if (invoice.getStatus() != Invoice.InvoiceStatus.APPROVED) {
                    throw new RuntimeException("Access denied: Finance can only view approved invoices");
                }
            }
        }
        
        return convertToDTO(invoice);
    }

    /**
     * Creates a new invoice in the system with automatic GST calculation and invoice number generation.
     * 
     * This method performs the following operations:
     * - Validates vendor existence and ensures at least one invoice item is provided
     * - Calculates subtotal from all invoice items (quantity × unit price)
     * - Automatically calculates CGST, SGST, and IGST based on vendor GSTIN state code
     * - Generates a unique invoice number in format: INV-YYYYMMDD-XXXX
     * - Sets invoice status to PENDING to initiate approval workflow
     * - Determines required approval levels based on invoice total amount
     * - Saves invoice items with proper ordering
     * - Creates audit log entry for tracking
     * 
     * @param invoiceDTO The invoice data transfer object containing vendor ID, items, dates, etc.
     * @param userName The username of the user creating the invoice (for audit trail)
     * @param request HTTP servlet request for capturing IP address in audit log
     * @return InvoiceDTO containing the created invoice with generated invoice number and calculated amounts
     * @throws RuntimeException if vendor not found, items are missing, or validation fails
     */
    @Transactional
    public InvoiceDTO createInvoice(InvoiceDTO invoiceDTO, String userName, HttpServletRequest request) {
        Vendor vendor = vendorRepository.findById(invoiceDTO.getVendorId())
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + invoiceDTO.getVendorId()));

        // Validate items
        if (invoiceDTO.getItems() == null || invoiceDTO.getItems().isEmpty()) {
            throw new RuntimeException("At least one invoice item is required");
        }

        Invoice invoice = new Invoice();
        invoice.setVendor(vendor);
        invoice.setInvoiceDate(invoiceDTO.getInvoiceDate());
        invoice.setDueDate(invoiceDTO.getDueDate());
        invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        invoice.setCreatedBy(userName); // Track who created the invoice

        // Generate invoice number
        invoice.setInvoiceNumber(generateInvoiceNumber());

        // Calculate total amount from items
        BigDecimal totalAmountFromItems = invoiceDTO.getItems().stream()
                .map(item -> {
                    BigDecimal itemAmount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return itemAmount;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        invoice.setAmount(totalAmountFromItems);

        // Calculate GST (assuming same state for simplicity - can be enhanced)
        String vendorState = extractStateFromGstin(vendor.getGstin());
        GstService.GSTCalculationResult gstResult = gstService.calculateGST(
                totalAmountFromItems, vendorState, vendorState);
        invoice.setCgstAmount(gstResult.getCgstAmount());
        invoice.setSgstAmount(gstResult.getSgstAmount());
        invoice.setIgstAmount(gstResult.getIgstAmount());
        invoice.setTotalAmount(gstResult.getTotalAmount());

        // Determine approval levels based on amount
        Optional<ApprovalRule> rule = approvalRuleRepository.findApplicableRule(invoice.getTotalAmount());
        if (rule.isPresent()) {
            invoice.setCurrentApprovalLevel(0);
        } else {
            invoice.setCurrentApprovalLevel(0); // Auto-approve if no rule
        }

        invoice = invoiceRepository.save(invoice);

        // Save invoice items
        int order = 1;
        for (InvoiceItemDTO itemDTO : invoiceDTO.getItems()) {
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setDescription(itemDTO.getDescription());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setItemOrder(order++);
            // Amount will be calculated by @PrePersist
            invoiceItemRepository.save(item);
        }

        // Log audit
        auditService.logAction(userName, "CREATE", "INVOICE", invoice.getId(),
                null, invoiceDTO, "Invoice created with " + invoiceDTO.getItems().size() + " items", request);

        return convertToDTO(invoice);
    }

    /**
     * Updates an existing invoice's details including items, dates, and amounts.
     * 
     * This method performs the following operations:
     * - Validates that only ADMIN role can update invoices
     * - Prevents editing of APPROVED, PAID, or PARTIALLY_PAID invoices
     * - Recalculates GST when invoice items or amounts are changed
     * - Resets invoice status to PENDING if it was previously REJECTED
     * - Updates invoice items by deleting old items and creating new ones
     * - Creates audit log entry with old and new values for tracking changes
     * 
     * @param id The unique identifier of the invoice to update
     * @param invoiceDTO The updated invoice data containing new items, dates, or amounts
     * @param userName The username of the user updating the invoice (for audit trail)
     * @param request HTTP servlet request for capturing IP address in audit log
     * @return InvoiceDTO containing the updated invoice with recalculated amounts
     * @throws RuntimeException if invoice not found, access denied, or invoice status prevents editing
     */
    @Transactional
    public InvoiceDTO updateInvoice(Long id, InvoiceDTO invoiceDTO, String userName, HttpServletRequest request) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));

        // Check access: Only ADMIN can update invoices
        if (!securityUtil.isAdmin()) {
            throw new RuntimeException("Access denied: Only ADMIN can update invoices");
        }

        // Prevent editing of approved, paid, or partially paid invoices
        if (invoice.getStatus() == Invoice.InvoiceStatus.APPROVED ||
            invoice.getStatus() == Invoice.InvoiceStatus.PAID ||
            invoice.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID) {
            throw new RuntimeException("Cannot edit invoice with status: " + invoice.getStatus() + 
                    ". Only PENDING, REJECTED, CREATED, OVERDUE, or ESCALATED invoices can be edited.");
        }

        InvoiceDTO oldDTO = convertToDTO(invoice);

        // If invoice was rejected, reset status to PENDING when edited
        if (invoice.getStatus() == Invoice.InvoiceStatus.REJECTED) {
            invoice.setStatus(Invoice.InvoiceStatus.PENDING);
            invoice.setCurrentApprovalLevel(0);
        }

        // Update items if provided
        if (invoiceDTO.getItems() != null && !invoiceDTO.getItems().isEmpty()) {
            // Clear existing items collection first (orphanRemoval will handle deletion)
            // Initialize collection if null
            if (invoice.getItems() == null) {
                invoice.setItems(new ArrayList<>());
            } else {
                invoice.getItems().clear();
            }
            
            // Flush to ensure deletions are processed before creating new items
            invoiceRepository.flush();
            
            // Calculate total amount from new items
            BigDecimal totalAmountFromItems = invoiceDTO.getItems().stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            invoice.setAmount(totalAmountFromItems);
            
            // Recalculate GST
            Vendor vendor = invoice.getVendor();
            String vendorState = extractStateFromGstin(vendor.getGstin());
            GstService.GSTCalculationResult gstResult = gstService.calculateGST(
                    totalAmountFromItems, vendorState, vendorState);
            invoice.setCgstAmount(gstResult.getCgstAmount());
            invoice.setSgstAmount(gstResult.getSgstAmount());
            invoice.setIgstAmount(gstResult.getIgstAmount());
            invoice.setTotalAmount(gstResult.getTotalAmount());
            
            // Create and add new items to the collection
            int order = 1;
            for (InvoiceItemDTO itemDTO : invoiceDTO.getItems()) {
                InvoiceItem item = new InvoiceItem();
                item.setInvoice(invoice);
                item.setDescription(itemDTO.getDescription());
                item.setQuantity(itemDTO.getQuantity());
                item.setUnitPrice(itemDTO.getUnitPrice());
                item.setItemOrder(order++);
                // Add to collection - orphanRemoval will handle persistence
                invoice.getItems().add(item);
            }
        } else if (invoiceDTO.getAmount() != null) {
            // Backward compatibility: if items not provided, use amount
            invoice.setAmount(invoiceDTO.getAmount());
            Vendor vendor = invoice.getVendor();
            String vendorState = extractStateFromGstin(vendor.getGstin());
            GstService.GSTCalculationResult gstResult = gstService.calculateGST(
                    invoiceDTO.getAmount(), vendorState, vendorState);
            invoice.setCgstAmount(gstResult.getCgstAmount());
            invoice.setSgstAmount(gstResult.getSgstAmount());
            invoice.setIgstAmount(gstResult.getIgstAmount());
            invoice.setTotalAmount(gstResult.getTotalAmount());
        }

        if (invoiceDTO.getInvoiceDate() != null) {
            invoice.setInvoiceDate(invoiceDTO.getInvoiceDate());
        }
        if (invoiceDTO.getDueDate() != null) {
            invoice.setDueDate(invoiceDTO.getDueDate());
        }

        invoice = invoiceRepository.save(invoice);

        // Log audit
        auditService.logAction(userName, "UPDATE", "INVOICE", invoice.getId(),
                oldDTO, convertToDTO(invoice), "Invoice updated", request);

        return convertToDTO(invoice);
    }


    /**
     * Approves an invoice at a specific approval level in the multi-level approval workflow.
     * 
     * This method performs comprehensive validation and approval processing:
     * - Validates that the user hasn't already approved this invoice (prevents duplicate approvals)
     * - Ensures the approval level is sequential (must be currentLevel + 1)
     * - Checks if the level has already been approved by another user
     * - Creates an approval record with approval level, approver, status, and comments
     * - Updates invoice's current approval level
     * - Automatically sets invoice status to APPROVED when all required levels are completed
     * - Keeps invoice status as PENDING if more approvals are needed
     * - Creates audit log entry for tracking approval actions
     * 
     * The required approval levels are determined by the invoice amount and applicable approval rules.
     * 
     * @param invoiceId The unique identifier of the invoice to approve
     * @param level The approval level (must be currentLevel + 1)
     * @param approvedBy The username of the user approving the invoice
     * @param comments Optional comments explaining the approval decision
     * @param request HTTP servlet request for capturing IP address in audit log
     * @return InvoiceDTO containing the updated invoice with new approval status
     * @throws RuntimeException if validation fails, duplicate approval detected, or invalid level provided
     */
    @Transactional
    public InvoiceDTO approveInvoice(Long invoiceId, Integer level, String approvedBy, String comments, HttpServletRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        // Validation: Check if user has already approved this invoice
        if (invoiceApprovalRepository.existsByInvoiceAndApprovedBy(invoice, approvedBy)) {
            // User already approved - return current invoice state (still pending if not fully approved)
            return convertToDTO(invoice);
        }

        // Validation: Check if this level has already been approved
        if (invoiceApprovalRepository.existsByInvoiceAndApprovalLevel(invoice, level)) {
            // Level already approved by another user - return current invoice state
            // Invoice remains PENDING if not all required levels are completed
            return convertToDTO(invoice);
        }

        // Validation: Check if the level is correct (should be currentLevel + 1)
        Integer currentLevel = invoice.getCurrentApprovalLevel() != null ? invoice.getCurrentApprovalLevel() : 0;
        if (level != currentLevel + 1) {
            throw new RuntimeException("Invalid approval level. Expected level " + (currentLevel + 1) + " but received " + level);
        }

        // Create approval record
        InvoiceApproval approval = new InvoiceApproval();
        approval.setInvoice(invoice);
        approval.setApprovalLevel(level);
        approval.setApprovedBy(approvedBy);
        approval.setStatus(InvoiceApproval.ApprovalStatus.APPROVED);
        approval.setComments(comments);
        invoiceApprovalRepository.save(approval);

        // Check if all required approvals are done
        Optional<ApprovalRule> rule = approvalRuleRepository.findApplicableRule(invoice.getTotalAmount());
        if (rule.isPresent()) {
            int requiredLevels = rule.get().getApprovalLevels();
            // Frontend already passes the next level (currentLevel + 1), so use it directly
            invoice.setCurrentApprovalLevel(level);
            
            if (invoice.getCurrentApprovalLevel() >= requiredLevels) {
                invoice.setStatus(Invoice.InvoiceStatus.APPROVED);
            } else {
                // Ensure status remains PENDING if not all approvals are done
                invoice.setStatus(Invoice.InvoiceStatus.PENDING);
            }
        } else {
            // No rule found - auto-approve
            invoice.setStatus(Invoice.InvoiceStatus.APPROVED);
        }

        invoice = invoiceRepository.save(invoice);

        // Log audit
        auditService.logAction(approvedBy, "APPROVE", "INVOICE", invoiceId,
                null, convertToDTO(invoice), "Invoice approved at level " + level, request);

        return convertToDTO(invoice);
    }

    /**
     * Rejects an invoice, stopping the approval workflow.
     * 
     * This method:
     * - Changes invoice status to REJECTED immediately
     * - Creates a rejection record with rejection comments for documentation
     * - Records the rejection at the current approval level
     * - Creates audit log entry for tracking rejection actions
     * 
     * Rejection comments are required to document the reason for rejection.
     * Once rejected, an invoice can be edited and resubmitted (status resets to PENDING).
     * 
     * @param invoiceId The unique identifier of the invoice to reject
     * @param rejectedBy The username of the user rejecting the invoice
     * @param comments Required comments explaining the rejection reason
     * @param request HTTP servlet request for capturing IP address in audit log
     * @return InvoiceDTO containing the updated invoice with REJECTED status
     * @throws RuntimeException if invoice not found
     */
    @Transactional
    public InvoiceDTO rejectInvoice(Long invoiceId, String rejectedBy, String comments, HttpServletRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        invoice.setStatus(Invoice.InvoiceStatus.REJECTED);
        invoice = invoiceRepository.save(invoice);

        // Create rejection record
        InvoiceApproval approval = new InvoiceApproval();
        approval.setInvoice(invoice);
        approval.setApprovalLevel(invoice.getCurrentApprovalLevel());
        approval.setApprovedBy(rejectedBy);
        approval.setStatus(InvoiceApproval.ApprovalStatus.REJECTED);
        approval.setComments(comments);
        invoiceApprovalRepository.save(approval);

        // Log audit
        auditService.logAction(rejectedBy, "REJECT", "INVOICE", invoiceId,
                null, convertToDTO(invoice), "Invoice rejected: " + comments, request);

        return convertToDTO(invoice);
    }

    /**
     * Retrieves all invoices that have passed their due date and are marked as overdue.
     * 
     * Overdue invoices are those where the due date is in the past and the invoice
     * has not been fully paid. This method is useful for:
     * - Tracking payment delays
     * - Managing collections
     * - Generating overdue reports
     * 
     * @return List of InvoiceDTO objects representing overdue invoices
     */
    public List<InvoiceDTO> getOverdueInvoices() {
        return invoiceRepository.findAllOverdueInvoices().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Marks invoices as overdue based on their due dates.
     * 
     * This method is typically called by a scheduled job to:
     * - Find all invoices with due dates in the past
     * - Set the isOverdue flag to true
     * - Update invoice status to OVERDUE
     * 
     * Should be executed daily to keep invoice statuses current.
     * 
     * @param today The current date to compare against invoice due dates
     */
    @Transactional
    public void markOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> overdueInvoices = invoiceRepository.findOverdueInvoices(today);
        
        for (Invoice invoice : overdueInvoices) {
            invoice.setIsOverdue(true);
            invoice.setStatus(Invoice.InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
        }
    }

    /**
     * Escalates overdue invoices by incrementing their escalation level.
     * 
     * This method:
     * - Finds all invoices currently marked as overdue
     * - Increments the escalation level for each overdue invoice
     * - Updates invoice status to ESCALATED
     * - Initializes escalation level to 0 if not previously set
     * 
     * Escalation levels help track how long an invoice has been overdue
     * and can trigger different actions based on escalation severity.
     * 
     * Typically called by a scheduled job for invoices that remain overdue
     * after a certain period.
     */
    @Transactional
    public void escalateOverdueInvoices() {
        List<Invoice> overdueInvoices = invoiceRepository.findAllOverdueInvoices();
        
        for (Invoice invoice : overdueInvoices) {
            if (invoice.getEscalationLevel() == null) {
                invoice.setEscalationLevel(0);
            }
            invoice.setEscalationLevel(invoice.getEscalationLevel() + 1);
            invoice.setStatus(Invoice.InvoiceStatus.ESCALATED);
            invoiceRepository.save(invoice);
        }
    }

    /**
     * Generates a unique invoice number in the format: INV-YYYYMMDD-XXXX
     * 
     * Format breakdown:
     * - INV: Prefix indicating invoice
     * - YYYYMMDD: Current date in year-month-day format
     * - XXXX: Random 4-digit sequence number
     * 
     * Example: INV-20241215-1234
     * 
     * @return A unique invoice number string
     */
    private String generateInvoiceNumber() {
        String prefix = "INV";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequence = String.format("%04d", (int)(Math.random() * 10000));
        return prefix + "-" + dateStr + "-" + sequence;
    }

    /**
     * Extracts the state code from a GSTIN (GST Identification Number).
     * 
     * GSTIN format: The first two digits represent the state code.
     * If GSTIN is null or invalid, defaults to "27" (Maharashtra state code).
     * 
     * @param gstin The GST Identification Number
     * @return Two-digit state code extracted from GSTIN, or "27" as default
     */
    private String extractStateFromGstin(String gstin) {
        if (gstin == null || gstin.length() < 2) {
            return "27"; // Default to Maharashtra
        }
        return gstin.substring(0, 2);
    }
    
    /**
     * Retrieves invoices created within a specific date range.
     * 
     * This method is useful for:
     * - Generating period-based reports
     * - Filtering invoices by creation date
     * - Financial period analysis
     * 
     * @param startDate The start date of the range (inclusive)
     * @param endDate The end date of the range (inclusive)
     * @return List of InvoiceDTO objects created within the specified date range
     */
    public List<InvoiceDTO> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findInvoicesByDateRange(startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves detailed approval workflow information for a specific invoice.
     * 
     * This method provides comprehensive approval status including:
     * - Current approval level progress
     * - Required approval levels based on invoice amount and approval rules
     * - Remaining approvals needed to complete the workflow
     * - Whether the invoice is fully approved
     * - Applicable approval rule amount range
     * 
     * This information is essential for displaying approval progress bars
     * and status indicators in the user interface.
     * 
     * @param invoiceId The unique identifier of the invoice
     * @return InvoiceApprovalInfoDTO containing all approval workflow details
     * @throws RuntimeException if invoice not found
     */
    public InvoiceApprovalInfoDTO getInvoiceApprovalInfo(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
        
        InvoiceApprovalInfoDTO info = new InvoiceApprovalInfoDTO();
        info.setCurrentApprovalLevel(invoice.getCurrentApprovalLevel() != null ? invoice.getCurrentApprovalLevel() : 0);
        
        // Find applicable approval rule
        Optional<ApprovalRule> rule = approvalRuleRepository.findApplicableRule(invoice.getTotalAmount());
        
        if (rule.isPresent()) {
            ApprovalRule approvalRule = rule.get();
            info.setRequiredApprovalLevels(approvalRule.getApprovalLevels());
            info.setRemainingApprovals(Math.max(0, approvalRule.getApprovalLevels() - info.getCurrentApprovalLevel()));
            info.setIsFullyApproved(info.getCurrentApprovalLevel() >= approvalRule.getApprovalLevels());
            info.setApprovalRuleRange(String.format("₹%,.2f - ₹%,.2f", 
                approvalRule.getMinAmount(), approvalRule.getMaxAmount()));
        } else {
            // No rule found - auto-approved or manual review
            info.setRequiredApprovalLevels(0);
            info.setRemainingApprovals(0);
            info.setIsFullyApproved(true);
            info.setApprovalRuleRange("No rule applicable");
        }
        
        return info;
    }

    private InvoiceDTO convertToDTO(Invoice invoice) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(invoice.getId());
        dto.setVendorId(invoice.getVendor().getId());
        dto.setVendorName(invoice.getVendor().getName());
        dto.setAmount(invoice.getAmount());
        dto.setCgstAmount(invoice.getCgstAmount());
        dto.setSgstAmount(invoice.getSgstAmount());
        dto.setIgstAmount(invoice.getIgstAmount());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setInvoiceDate(invoice.getInvoiceDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setStatus(invoice.getStatus());
        dto.setCurrentApprovalLevel(invoice.getCurrentApprovalLevel());
        dto.setIsOverdue(invoice.getIsOverdue());
        dto.setEscalationLevel(invoice.getEscalationLevel());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());
        
        // Convert items
        if (invoice.getItems() != null && !invoice.getItems().isEmpty()) {
            dto.setItems(invoice.getItems().stream()
                    .map(this::convertItemToDTO)
                    .collect(Collectors.toList()));
        } else {
            dto.setItems(List.of()); // Empty list if no items
        }
        
        return dto;
    }

    private InvoiceItemDTO convertItemToDTO(InvoiceItem item) {
        InvoiceItemDTO dto = new InvoiceItemDTO();
        dto.setId(item.getId());
        dto.setDescription(item.getDescription());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setAmount(item.getAmount());
        dto.setItemOrder(item.getItemOrder());
        return dto;
    }
}


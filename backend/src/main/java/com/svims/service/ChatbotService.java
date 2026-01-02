package com.svims.service;

import com.svims.dto.ChatbotRequest;
import com.svims.dto.ChatbotResponse;
import com.svims.dto.InvoiceDTO;
import com.svims.dto.PaymentDTO;
import com.svims.entity.Invoice;
import com.svims.entity.InvoiceApproval;
import com.svims.entity.ApprovalRule;
import com.svims.entity.Vendor;
import com.svims.entity.AuditLog;
import com.svims.repository.*;
import java.util.Set;
import com.svims.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * AI-Powered Chatbot Service
 * Provides role-aware intelligent responses about the invoice management system
 */
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final PaymentService paymentService;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceApprovalRepository invoiceApprovalRepository;
    private final ApprovalRuleRepository approvalRuleRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final AuditLogRepository auditLogRepository;
    private final GenAIService genAIService;
    private final SecurityUtil securityUtil;

    /**
     * Process user message and return intelligent response
     * Uses GenAI with RAG (Retrieval Augmented Generation) pattern
     */
    public ChatbotResponse processMessage(ChatbotRequest request) {
        String message = request.getMessage();
        String role = request.getRole();
        String username = request.getUsername();

        // Extract user's primary role (remove ROLE_ prefix if present)
        String userRole = role.replace("ROLE_", "").toUpperCase();

        // Step 1: Check if question is from predefined list
        boolean isPredefinedQuestion = isPredefinedQuestion(message, userRole);
        
        // Step 2: If NOT a predefined question, return generic help message
        if (!isPredefinedQuestion) {
            return new ChatbotResponse(
                buildGenericHelpMessage(userRole),
                role, null, null
            );
        }

        // Step 3: For predefined questions, fetch actual data
        // Check if GenAI is enabled
        if (!genAIService.isGenAIEnabled()) {
            // Even without GenAI, try to fetch and show data
            List<Map<String, Object>> structuredData = fetchStructuredData(message.toLowerCase(), userRole, username);
            String dataResponse = generateResponseFromData(message, userRole, structuredData);
            if (dataResponse != null && !dataResponse.isEmpty()) {
                return new ChatbotResponse(dataResponse, role, structuredData, null);
            }
            return new ChatbotResponse(
                "I'm sorry, but the AI assistant is currently not available. Please ensure GenAI is enabled and configured with a valid API key.",
                role, null, null
            );
        }

        // Step 4: Retrieve relevant data from database (RAG - Retrieval)
        String contextData = retrieveContextData(message.toLowerCase(), userRole, username);
        
        // Step 5: Fetch structured data based on the question
        List<Map<String, Object>> structuredData = fetchStructuredData(message.toLowerCase(), userRole, username);

        // Step 6: Generate response using GenAI
        try {
            // Always try to generate response from data first (more reliable)
            String dataResponse = null;
            if (structuredData != null && !structuredData.isEmpty()) {
                dataResponse = generateResponseFromData(message, userRole, structuredData);
            }
            
            // Try GenAI response
            String aiResponse = genAIService.generateResponse(message, userRole, contextData);
            
            // Priority: Use data-based response if available, otherwise use AI response
            String finalResponse = null;
            if (dataResponse != null && !dataResponse.isEmpty()) {
                finalResponse = dataResponse;
                // Enhance with AI if available
                if (aiResponse != null && !aiResponse.isEmpty() && !aiResponse.contains("I'm here to help")) {
                    finalResponse = aiResponse + "\n\n" + dataResponse;
                }
            } else if (aiResponse != null && !aiResponse.isEmpty()) {
                finalResponse = aiResponse;
            }
            
            // If still no response, create one from context data
            if (finalResponse == null || finalResponse.isEmpty()) {
                if (contextData != null && !contextData.isEmpty()) {
                    finalResponse = "Based on the current system data:\n\n" + contextData + 
                                   "\n\nYou can view detailed information in the respective sections of the application.";
                } else {
                    finalResponse = generateResponseFromData(message, userRole, structuredData);
                }
            }
            
            // Final fallback
            if (finalResponse == null || finalResponse.isEmpty()) {
                finalResponse = "I found the following information. See the details below.";
            }
            
            return new ChatbotResponse(finalResponse, role, structuredData, 
                "Is there anything else you'd like to know?");
        } catch (Exception e) {
            // Even on error, if we have data, show it with a message
            if (structuredData != null && !structuredData.isEmpty()) {
                String errorResponse = generateResponseFromData(message, userRole, structuredData);
                if (errorResponse != null && !errorResponse.isEmpty()) {
                    return new ChatbotResponse(
                        errorResponse,
                        role, structuredData, null
                    );
                }
            }
            // Try context data
            if (contextData != null && !contextData.isEmpty()) {
                return new ChatbotResponse(
                    "Based on the current system data:\n\n" + contextData,
                    role, structuredData, null
                );
            }
            return new ChatbotResponse(
                "I encountered an error, but I'm here to help. Please try asking your question again or check the system sections for detailed information.",
                role, structuredData, null
            );
        }
    }

    /**
     * Retrieve relevant context data for RAG (Retrieval Augmented Generation)
     * Fetches actual data from database to provide context to GenAI
     */
    private String retrieveContextData(String message, String userRole, String username) {
        StringBuilder context = new StringBuilder();

        try {
            // Get invoice counts based on role
            if (userRole.equals("USER")) {
                long pending = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING).size();
                long approved = invoiceRepository.findByStatus(Invoice.InvoiceStatus.APPROVED).size();
                context.append("Pending: ").append(pending).append(" invoices, Approved: ").append(approved).append(" invoices\n");
            } else if (userRole.equals("MANAGER") || userRole.equals("ADMIN")) {
                long pending = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING).size();
                context.append("Pending Approvals: ").append(pending).append(" invoices\n");
            } else if (userRole.equals("FINANCE") || userRole.equals("ADMIN")) {
                long approved = invoiceRepository.findByStatus(Invoice.InvoiceStatus.APPROVED).size();
                context.append("Approved Invoices Ready for Payment: ").append(approved).append(" invoices\n");
            }

            // Get payment data for FINANCE/ADMIN
            if (userRole.equals("FINANCE") || userRole.equals("ADMIN")) {
                List<PaymentDTO> payments = paymentService.getAllPayments();
                BigDecimal totalPaid = payments.stream()
                        .map(PaymentDTO::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                context.append("Total Payments: ").append(payments.size()).append(", Total Paid: ₹").append(totalPaid).append("\n");
            }

            // Get system summary for ADMIN
            if (userRole.equals("ADMIN")) {
                context.append("System: ")
                       .append(invoiceRepository.count()).append(" invoices, ")
                       .append(vendorRepository.count()).append(" vendors, ")
                       .append(userRepository.count()).append(" users\n");
            }
            
            // Get vendor information if question is about vendors
            if (message.contains("vendor") || message.contains("registered")) {
                long vendorCount = vendorRepository.count();
                long activeVendors = vendorRepository.findByStatus(Vendor.VendorStatus.ACTIVE).size();
                long highRiskVendors = vendorRepository.findHighRiskVendors(50.0).size();
                
                context.append("Vendors: Total: ").append(vendorCount)
                       .append(", Active: ").append(activeVendors)
                       .append(", High-Risk: ").append(highRiskVendors).append("\n");
            }
        } catch (Exception e) {
            // Continue with empty context if data retrieval fails
        }

        return context.toString();
    }

    /**
     * Fetch structured data from database based on user's question
     * Returns data in a format that can be displayed in tables/cards in UI
     * Only fetches data relevant to the specific question asked
     */
    private List<Map<String, Object>> fetchStructuredData(String message, String userRole, String username) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        
        try {
            String lowerMessage = message.toLowerCase();
            
            // Determine what type of data is being asked for - be more precise
            boolean askingForInvoices = (lowerMessage.contains("invoice") && 
                (lowerMessage.contains("show") || lowerMessage.contains("list") || 
                 lowerMessage.contains("view") || lowerMessage.contains("which") ||
                 lowerMessage.contains("status") || lowerMessage.contains("history") ||
                 lowerMessage.contains("track") || lowerMessage.contains("pending") ||
                 lowerMessage.contains("approved") || lowerMessage.contains("overdue") ||
                 lowerMessage.contains("high-value") || lowerMessage.contains("high value") ||
                 lowerMessage.contains("immediate") || lowerMessage.contains("attention") ||
                 lowerMessage.contains("partially") || lowerMessage.contains("partial"))) ||
                (lowerMessage.contains("pending") && !lowerMessage.contains("payment")) ||
                (lowerMessage.contains("approved") && !lowerMessage.contains("payment"));
            
            boolean askingForPayments = (lowerMessage.contains("payment") && 
                (lowerMessage.contains("show") || lowerMessage.contains("list") || 
                 lowerMessage.contains("view") || lowerMessage.contains("which") ||
                 lowerMessage.contains("history") || lowerMessage.contains("today") ||
                 lowerMessage.contains("month") || lowerMessage.contains("process") ||
                 lowerMessage.contains("record") || lowerMessage.contains("statistic"))) ||
                (lowerMessage.contains("paid") && (lowerMessage.contains("show") || 
                 lowerMessage.contains("how many") || lowerMessage.contains("total")));
            
            boolean askingForVendors = lowerMessage.contains("vendor") && 
                (lowerMessage.contains("show") || lowerMessage.contains("list") || 
                 lowerMessage.contains("view") || lowerMessage.contains("how many") ||
                 lowerMessage.contains("registered") || lowerMessage.contains("high-risk") ||
                 lowerMessage.contains("risk") || (lowerMessage.contains("pending") && 
                 lowerMessage.contains("payment")));
            
            boolean askingForApprovalHistory = lowerMessage.contains("approval") && 
                (lowerMessage.contains("history") || lowerMessage.contains("track") || 
                 lowerMessage.contains("who") || lowerMessage.contains("else"));
            
            boolean askingForApprovalRules = lowerMessage.contains("approval") && 
                (lowerMessage.contains("level") || lowerMessage.contains("requirement") || 
                 lowerMessage.contains("rule"));
            
            boolean askingForStatistics = lowerMessage.contains("statistic") || 
                lowerMessage.contains("summary") || lowerMessage.contains("how many") ||
                lowerMessage.contains("number of") || lowerMessage.contains("total") ||
                lowerMessage.contains("count") || lowerMessage.contains("revenue") ||
                lowerMessage.contains("amount") || lowerMessage.contains("overview") ||
                lowerMessage.contains("analytics") || lowerMessage.contains("trend") ||
                lowerMessage.contains("health") || lowerMessage.contains("average") ||
                lowerMessage.contains("outstanding") || (lowerMessage.contains("active") && 
                lowerMessage.contains("user"));
            
            // Fetch invoices ONLY if specifically asking about invoices
            if (askingForInvoices && !askingForStatistics) {
                
                List<Invoice> invoices;
                
                // Get invoices based on role and question
                if (userRole.equals("USER")) {
                    invoices = invoiceRepository.findAll().stream()
                        .filter(inv -> inv.getCreatedBy() != null && inv.getCreatedBy().equals(username))
                        .collect(Collectors.toList());
                } else if (userRole.equals("MANAGER")) {
                    invoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING);
                } else if (userRole.equals("FINANCE")) {
                    invoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.APPROVED);
                } else { // ADMIN
                    invoices = invoiceRepository.findAll();
                }
                
                // Filter based on question
                if (lowerMessage.contains("pending")) {
                    invoices = invoices.stream()
                        .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PENDING)
                        .limit(10) // Limit to 10 for display
                        .collect(Collectors.toList());
                } else if (lowerMessage.contains("approved")) {
                    invoices = invoices.stream()
                        .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.APPROVED)
                        .limit(10)
                        .collect(Collectors.toList());
                } else if (lowerMessage.contains("overdue")) {
                    invoices = invoices.stream()
                        .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.OVERDUE)
                        .limit(10)
                        .collect(Collectors.toList());
                } else if (lowerMessage.contains("high-value") || lowerMessage.contains("high value")) {
                    // Sort by total amount descending and get top 10
                    invoices = invoices.stream()
                        .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PENDING)
                        .sorted((a, b) -> {
                            BigDecimal amtA = a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO;
                            BigDecimal amtB = b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO;
                            return amtB.compareTo(amtA);
                        })
                        .limit(10)
                        .collect(Collectors.toList());
                } else if (lowerMessage.contains("immediate") || lowerMessage.contains("attention")) {
                    // Get overdue or high-value pending invoices
                    invoices = invoices.stream()
                        .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.OVERDUE || 
                                     (inv.getStatus() == Invoice.InvoiceStatus.PENDING && 
                                      inv.getTotalAmount() != null && 
                                      inv.getTotalAmount().compareTo(new BigDecimal("100000")) > 0))
                        .limit(10)
                        .collect(Collectors.toList());
                } else if (lowerMessage.contains("partially") || lowerMessage.contains("partial")) {
                    invoices = invoices.stream()
                        .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID)
                        .limit(10)
                        .collect(Collectors.toList());
                } else {
                    // Show recent invoices (limit to 5)
                    invoices = invoices.stream()
                        .limit(5)
                        .collect(Collectors.toList());
                }
                
                // Convert to map format for frontend
                for (Invoice invoice : invoices) {
                    Map<String, Object> invoiceData = new HashMap<>();
                    invoiceData.put("type", "invoice");
                    invoiceData.put("id", invoice.getId());
                    invoiceData.put("invoiceNumber", invoice.getInvoiceNumber());
                    invoiceData.put("vendorName", invoice.getVendor() != null ? invoice.getVendor().getName() : "");
                    invoiceData.put("amount", invoice.getAmount() != null ? invoice.getAmount().toString() : "0");
                    invoiceData.put("totalAmount", invoice.getTotalAmount() != null ? invoice.getTotalAmount().toString() : "0");
                    invoiceData.put("status", invoice.getStatus() != null ? invoice.getStatus().toString() : "");
                    invoiceData.put("invoiceDate", invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().toString() : "");
                    invoiceData.put("dueDate", invoice.getDueDate() != null ? invoice.getDueDate().toString() : "");
                    dataList.add(invoiceData);
                }
            }
            
            // Fetch payments ONLY if specifically asking about payments
            if (askingForPayments && !askingForStatistics) {
                List<PaymentDTO> payments = paymentService.getAllPayments();
                
                // Filter based on question
                if (lowerMessage.contains("history") && lowerMessage.contains("invoice")) {
                    // Payment history for a specific invoice - extract invoice ID if mentioned
                    // For now, show all payments grouped by invoice
                    // In a real scenario, you might extract invoice number/ID from the message
                    payments = payments.stream()
                        .limit(20) // Show more for history
                        .collect(Collectors.toList());
                } else if (lowerMessage.contains("today")) {
                    // Filter payments from today
                    LocalDate today = LocalDate.now();
                    payments = payments.stream()
                        .filter(p -> p.getPaymentDate() != null && 
                                   p.getPaymentDate().toLocalDate().equals(today))
                        .collect(Collectors.toList());
                } else if (lowerMessage.contains("month")) {
                    // Filter payments from current month
                    LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
                    payments = payments.stream()
                        .filter(p -> p.getPaymentDate() != null && 
                                   p.getPaymentDate().toLocalDate().isAfter(firstDayOfMonth.minusDays(1)))
                        .collect(Collectors.toList());
                } else {
                    // Limit to recent 10 payments
                    payments = payments.stream()
                        .limit(10)
                        .collect(Collectors.toList());
                }
                
                // Convert to map format for frontend
                for (PaymentDTO payment : payments) {
                    Map<String, Object> paymentData = new HashMap<>();
                    paymentData.put("type", "payment");
                    paymentData.put("id", payment.getId());
                    paymentData.put("invoiceId", payment.getInvoiceId());
                    paymentData.put("amount", payment.getAmount() != null ? payment.getAmount().toString() : "0");
                    paymentData.put("paymentDate", payment.getPaymentDate() != null ? payment.getPaymentDate().toString() : "");
                    paymentData.put("paymentMethod", payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "");
                    paymentData.put("transactionReference", payment.getTransactionReference() != null ? payment.getTransactionReference() : "");
                    dataList.add(paymentData);
                }
            }
            
            // Fetch vendor data ONLY if specifically asking about vendors
            if (askingForVendors && !askingForStatistics) {
                // Check if asking for list or just count
                if (lowerMessage.contains("show") || lowerMessage.contains("list") || 
                    lowerMessage.contains("all") || lowerMessage.contains("view") ||
                    (lowerMessage.contains("pending") && lowerMessage.contains("payment"))) {
                    // Fetch vendor list (only for ADMIN, MANAGER, FINANCE)
                    if (userRole.equals("ADMIN") || userRole.equals("MANAGER") || userRole.equals("FINANCE")) {
                        List<Vendor> vendors;
                        if (lowerMessage.contains("high-risk") || lowerMessage.contains("risk")) {
                            vendors = vendorRepository.findHighRiskVendors(50.0);
                        } else if (lowerMessage.contains("pending") && lowerMessage.contains("payment")) {
                            // Find vendors with pending payments (approved invoices not fully paid)
                            List<Invoice> approvedInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.APPROVED);
                            Set<Long> vendorIdsWithPending = approvedInvoices.stream()
                                .map(inv -> inv.getVendor() != null ? inv.getVendor().getId() : null)
                                .filter(id -> id != null)
                                .collect(Collectors.toSet());
                            vendors = vendorRepository.findAll().stream()
                                .filter(v -> vendorIdsWithPending.contains(v.getId()))
                                .collect(Collectors.toList());
                        } else {
                            vendors = vendorRepository.findAll();
                        }
                        
                        // Limit to 10 for display
                        vendors = vendors.stream().limit(10).collect(Collectors.toList());
                        
                        for (Vendor vendor : vendors) {
                            Map<String, Object> vendorData = new HashMap<>();
                            vendorData.put("type", "vendor");
                            vendorData.put("id", vendor.getId());
                            vendorData.put("name", vendor.getName());
                            vendorData.put("email", vendor.getEmail());
                            vendorData.put("gstin", vendor.getGstin() != null ? vendor.getGstin() : "");
                            vendorData.put("status", vendor.getStatus() != null ? vendor.getStatus().toString() : "");
                            vendorData.put("riskScore", vendor.getRiskScore() != null ? vendor.getRiskScore() : 0.0);
                            dataList.add(vendorData);
                        }
                    }
                }
            }
            
            // Fetch approval history ONLY if specifically asking about approval history
            if (askingForApprovalHistory) {
                // Fetch approval history for user
                List<InvoiceApproval> approvals = invoiceApprovalRepository.findAll();
                if (userRole.equals("MANAGER") || userRole.equals("ADMIN")) {
                    // Filter by approver if MANAGER
                    if (userRole.equals("MANAGER")) {
                        approvals = approvals.stream()
                            .filter(ap -> ap.getApprovedBy().equals(username))
                            .limit(20)
                            .collect(Collectors.toList());
                    } else {
                        approvals = approvals.stream().limit(20).collect(Collectors.toList());
                    }
                    
                    for (InvoiceApproval approval : approvals) {
                        Map<String, Object> approvalData = new HashMap<>();
                        approvalData.put("type", "approval");
                        approvalData.put("id", approval.getId());
                        approvalData.put("invoiceId", approval.getInvoice().getId());
                        approvalData.put("invoiceNumber", approval.getInvoice().getInvoiceNumber());
                        approvalData.put("approvalLevel", approval.getApprovalLevel());
                        approvalData.put("approvedBy", approval.getApprovedBy());
                        approvalData.put("status", approval.getStatus() != null ? approval.getStatus().toString() : "");
                        approvalData.put("comments", approval.getComments() != null ? approval.getComments() : "");
                        approvalData.put("createdAt", approval.getCreatedAt() != null ? approval.getCreatedAt().toString() : "");
                        dataList.add(approvalData);
                    }
                }
            }
            
            // Fetch approval rules ONLY if specifically asking about approval rules
            if (askingForApprovalRules) {
                List<ApprovalRule> rules = approvalRuleRepository.findByIsActiveTrueOrderByPriorityAsc();
                for (ApprovalRule rule : rules) {
                    Map<String, Object> ruleData = new HashMap<>();
                    ruleData.put("type", "approvalRule");
                    ruleData.put("id", rule.getId());
                    ruleData.put("minAmount", rule.getMinAmount() != null ? rule.getMinAmount().toString() : "0");
                    ruleData.put("maxAmount", rule.getMaxAmount() != null ? rule.getMaxAmount().toString() : "0");
                    ruleData.put("approvalLevels", rule.getApprovalLevels());
                    ruleData.put("requiredRoles", rule.getRequiredRoles() != null ? rule.getRequiredRoles() : "");
                    ruleData.put("range", "₹" + rule.getMinAmount() + " - ₹" + rule.getMaxAmount());
                    dataList.add(ruleData);
                }
            }
            
            // Fetch statistics ONLY if specifically asking for statistics/summary
            if (askingForStatistics) {
                
                Map<String, Object> stats = new HashMap<>();
                stats.put("type", "statistics");
                
                // Only include stats relevant to the specific question
                boolean askingForUserCount = lowerMessage.contains("user") && 
                    (lowerMessage.contains("how many") || lowerMessage.contains("count"));
                boolean askingForVendorCount = lowerMessage.contains("vendor") && 
                    (lowerMessage.contains("how many") || lowerMessage.contains("count") || 
                     lowerMessage.contains("registered"));
                boolean askingForInvoiceCount = lowerMessage.contains("invoice") && 
                    (lowerMessage.contains("how many") || lowerMessage.contains("count") || 
                     lowerMessage.contains("created today"));
                boolean askingForRevenue = lowerMessage.contains("revenue") || 
                    (lowerMessage.contains("month") && lowerMessage.contains("amount"));
                boolean askingForAverage = lowerMessage.contains("average");
                boolean askingForOutstanding = lowerMessage.contains("outstanding");
                boolean askingForTodayPayments = lowerMessage.contains("today") && 
                    lowerMessage.contains("payment");
                boolean askingForMonthlyPayments = lowerMessage.contains("month") && 
                    lowerMessage.contains("payment");
                boolean askingForActiveUsers = lowerMessage.contains("active") && 
                    lowerMessage.contains("user");
                
                if (userRole.equals("ADMIN")) {
                    // Only add stats that are relevant to the question
                    if (askingForInvoiceCount || !askingForUserCount && !askingForVendorCount && 
                        !askingForRevenue && !askingForAverage && !askingForOutstanding && 
                        !askingForTodayPayments && !askingForMonthlyPayments && !askingForActiveUsers) {
                        stats.put("totalInvoices", invoiceRepository.count());
                        long pendingCount = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING).size();
                        long approvedCount = invoiceRepository.findByStatus(Invoice.InvoiceStatus.APPROVED).size();
                        long paidCount = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PAID).size();
                        stats.put("pendingInvoices", pendingCount);
                        stats.put("approvedInvoices", approvedCount);
                        stats.put("paidInvoices", paidCount);
                    }
                    
                    if (askingForVendorCount || lowerMessage.contains("vendor")) {
                        stats.put("totalVendors", vendorRepository.count());
                    }
                    
                    if (askingForUserCount || lowerMessage.contains("user")) {
                        stats.put("totalUsers", userRepository.count());
                    }
                    
                    if (askingForRevenue || lowerMessage.contains("revenue")) {
                        List<PaymentDTO> allPayments = paymentService.getAllPayments();
                        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
                        BigDecimal monthlyRevenue = allPayments.stream()
                            .filter(p -> p.getPaymentDate() != null && 
                                       p.getPaymentDate().toLocalDate().isAfter(firstDayOfMonth.minusDays(1)))
                            .map(PaymentDTO::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        stats.put("monthlyRevenue", monthlyRevenue.toString());
                    }
                    
                    if (askingForAverage || lowerMessage.contains("average")) {
                        List<Invoice> allInvoices = invoiceRepository.findAll();
                        if (allInvoices.size() > 0) {
                            BigDecimal totalInvoiceAmount = allInvoices.stream()
                                .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                            BigDecimal avgAmount = totalInvoiceAmount.divide(BigDecimal.valueOf(allInvoices.size()), 2, RoundingMode.HALF_UP);
                            stats.put("averageInvoiceAmount", avgAmount.toString());
                        }
                    }
                    
                    if (askingForOutstanding || lowerMessage.contains("outstanding")) {
                        List<Invoice> allInvoices = invoiceRepository.findAll();
                        List<PaymentDTO> allPayments = paymentService.getAllPayments();
                        BigDecimal totalPaid = allPayments.stream()
                            .map(PaymentDTO::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalInvoiceAmount = allInvoices.stream()
                            .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal outstandingAmount = totalInvoiceAmount.subtract(totalPaid);
                        stats.put("totalOutstandingAmount", outstandingAmount.toString());
                    }
                    
                    if (askingForInvoiceCount && lowerMessage.contains("today")) {
                        LocalDate today = LocalDate.now();
                        List<Invoice> allInvoices = invoiceRepository.findAll();
                        long todayInvoices = allInvoices.stream()
                            .filter(inv -> inv.getInvoiceDate() != null && inv.getInvoiceDate().equals(today))
                            .count();
                        stats.put("todayInvoices", todayInvoices);
                    }
                    
                    if (lowerMessage.contains("approval") && lowerMessage.contains("statistic")) {
                        long totalApprovals = invoiceApprovalRepository.count();
                        long approvedCount_approval = invoiceApprovalRepository.findAll().stream()
                            .filter(ap -> ap.getStatus() == InvoiceApproval.ApprovalStatus.APPROVED)
                            .count();
                        long rejectedCount = invoiceApprovalRepository.findAll().stream()
                            .filter(ap -> ap.getStatus() == InvoiceApproval.ApprovalStatus.REJECTED)
                            .count();
                        stats.put("totalApprovals", totalApprovals);
                        stats.put("approvedCount", approvedCount_approval);
                        stats.put("rejectedCount", rejectedCount);
                    }
                    
                    if (askingForActiveUsers) {
                        List<AuditLog> allLogs = auditLogRepository.findAll();
                        Map<String, Long> userActivityCount = allLogs.stream()
                            .collect(Collectors.groupingBy(
                                AuditLog::getUserName,
                                Collectors.counting()
                            ));
                        // Get top 5 most active users
                        List<Map.Entry<String, Long>> topUsers = userActivityCount.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(5)
                            .collect(Collectors.toList());
                        stats.put("mostActiveUsers", topUsers.stream()
                            .map(e -> e.getKey() + " (" + e.getValue() + " actions)")
                            .collect(Collectors.joining(", ")));
                    }
                    
                } else if (userRole.equals("MANAGER")) {
                    // Only include approval-related stats for managers
                    if (lowerMessage.contains("approval") && lowerMessage.contains("statistic")) {
                        long pendingCount = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING).size();
                        stats.put("pendingApprovals", pendingCount);
                        
                        // Approval statistics for manager
                        List<InvoiceApproval> managerApprovals = invoiceApprovalRepository.findAll().stream()
                            .filter(ap -> ap.getApprovedBy().equals(username))
                            .collect(Collectors.toList());
                        long managerApprovedCount = managerApprovals.stream()
                            .filter(ap -> ap.getStatus() == InvoiceApproval.ApprovalStatus.APPROVED)
                            .count();
                        long managerRejectedCount = managerApprovals.stream()
                            .filter(ap -> ap.getStatus() == InvoiceApproval.ApprovalStatus.REJECTED)
                            .count();
                        stats.put("myApprovals", managerApprovals.size());
                        stats.put("myApprovedCount", managerApprovedCount);
                        stats.put("myRejectedCount", managerRejectedCount);
                    } else {
                        long pendingCount = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PENDING).size();
                        stats.put("pendingApprovals", pendingCount);
                    }
                } else if (userRole.equals("FINANCE")) {
                    List<PaymentDTO> allPayments = paymentService.getAllPayments();
                    
                    // Only include stats relevant to the question
                    if (askingForTodayPayments || (lowerMessage.contains("today") && lowerMessage.contains("payment"))) {
                        LocalDate today = LocalDate.now();
                        long todayPayments = allPayments.stream()
                            .filter(p -> p.getPaymentDate() != null && 
                                       p.getPaymentDate().toLocalDate().equals(today))
                            .count();
                        BigDecimal todayPaid = allPayments.stream()
                            .filter(p -> p.getPaymentDate() != null && 
                                       p.getPaymentDate().toLocalDate().equals(today))
                            .map(PaymentDTO::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        stats.put("todayPayments", todayPayments);
                        stats.put("todayPaidAmount", todayPaid.toString());
                    }
                    
                    if (askingForMonthlyPayments || (lowerMessage.contains("month") && lowerMessage.contains("amount"))) {
                        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
                        BigDecimal monthlyPaid = allPayments.stream()
                            .filter(p -> p.getPaymentDate() != null && 
                                       p.getPaymentDate().toLocalDate().isAfter(firstDayOfMonth.minusDays(1)))
                            .map(PaymentDTO::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        stats.put("monthlyPaidAmount", monthlyPaid.toString());
                    }
                    
                    if (askingForOutstanding || lowerMessage.contains("outstanding")) {
                        List<Invoice> approvedInvoices = invoiceRepository.findByStatus(Invoice.InvoiceStatus.APPROVED);
                        BigDecimal approvedTotal = approvedInvoices.stream()
                            .map(inv -> inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalPaid = allPayments.stream()
                            .map(PaymentDTO::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal outstanding = approvedTotal.subtract(totalPaid);
                        stats.put("totalOutstandingAmount", outstanding.toString());
                    }
                    
                    // If asking for payment statistics in general
                    if (lowerMessage.contains("payment") && lowerMessage.contains("statistic")) {
                        long approvedCount = invoiceRepository.findByStatus(Invoice.InvoiceStatus.APPROVED).size();
                        long partiallyPaidCount = invoiceRepository.findByStatus(Invoice.InvoiceStatus.PARTIALLY_PAID).size();
                        BigDecimal totalPaid = allPayments.stream()
                            .map(PaymentDTO::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                        stats.put("approvedInvoices", approvedCount);
                        stats.put("partiallyPaidInvoices", partiallyPaidCount);
                        stats.put("totalPayments", allPayments.size());
                        stats.put("totalPaidAmount", totalPaid.toString());
                    }
                } else if (userRole.equals("USER")) {
                    // Get user's invoices directly from repository
                    List<Invoice> allInvoices = invoiceRepository.findAll();
                    List<Invoice> userInvoices = allInvoices.stream()
                        .filter(inv -> inv.getCreatedBy() != null && inv.getCreatedBy().equals(username))
                        .collect(Collectors.toList());
                    
                    long pendingCount = userInvoices.stream()
                        .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PENDING)
                        .count();
                    long approvedCount = userInvoices.stream()
                        .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.APPROVED)
                        .count();
                    stats.put("myPendingInvoices", pendingCount);
                    stats.put("myApprovedInvoices", approvedCount);
                    stats.put("myTotalInvoices", userInvoices.size());
                }
                
                dataList.add(stats);
            }
            
        } catch (Exception e) {
            // Log error but continue without structured data
            System.err.println("Error fetching structured data: " + e.getMessage());
        }
        
        return dataList;
    }

    /**
     * Generate a meaningful response from structured data when AI response is unavailable
     */
    private String generateResponseFromData(String message, String userRole, List<Map<String, Object>> structuredData) {
        StringBuilder response = new StringBuilder();
        String lowerMessage = message.toLowerCase();
        
        // Check for invoice data
        List<Map<String, Object>> invoiceData = structuredData.stream()
            .filter(item -> "invoice".equals(item.get("type")))
            .collect(Collectors.toList());
        
        if (!invoiceData.isEmpty()) {
            if (lowerMessage.contains("pending")) {
                response.append("Here are the pending invoices awaiting approval:\n\n");
                response.append("**Total Pending Invoices: ").append(invoiceData.size()).append("**\n\n");
            } else if (lowerMessage.contains("approved")) {
                response.append("Here are the approved invoices ready for payment:\n\n");
                response.append("**Total Approved Invoices: ").append(invoiceData.size()).append("**\n\n");
            } else if (lowerMessage.contains("overdue")) {
                response.append("Here are the overdue invoices:\n\n");
                response.append("**Total Overdue Invoices: ").append(invoiceData.size()).append("**\n\n");
            } else if (lowerMessage.contains("invoice") && (lowerMessage.contains("status") || lowerMessage.contains("history"))) {
                response.append("Here is your invoice information:\n\n");
                response.append("**Total Invoices: ").append(invoiceData.size()).append("**\n\n");
            } else {
                response.append("Here are the invoice details:\n\n");
                response.append("**Total: ").append(invoiceData.size()).append(" invoice(s)**\n\n");
            }
            
            // Add summary of invoice amounts if available
            if (invoiceData.size() > 0 && invoiceData.size() <= 5) {
                BigDecimal totalAmount = BigDecimal.ZERO;
                for (Map<String, Object> inv : invoiceData) {
                    String totalAmt = (String) inv.get("totalAmount");
                    if (totalAmt != null && !totalAmt.isEmpty()) {
                        try {
                            totalAmount = totalAmount.add(new BigDecimal(totalAmt));
                        } catch (Exception e) {
                            // Ignore parsing errors
                        }
                    }
                }
                if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    response.append("**Total Amount: ₹").append(totalAmount).append("**\n\n");
                }
            }
            
            response.append("See the detailed table below for complete information.");
        }
        
        // Check for payment data
        List<Map<String, Object>> paymentData = structuredData.stream()
            .filter(item -> "payment".equals(item.get("type")))
            .collect(Collectors.toList());
        
        if (!paymentData.isEmpty()) {
            if (response.length() > 0) response.append("\n\n");
            
            if (lowerMessage.contains("statistic") || lowerMessage.contains("summary")) {
                response.append("**Payment Statistics:**\n\n");
            } else {
                response.append("Here are the payment records:\n\n");
            }
            
            response.append("**Total Payments: ").append(paymentData.size()).append("**\n");
            
            // Calculate total paid amount
            BigDecimal totalPaid = BigDecimal.ZERO;
            for (Map<String, Object> payment : paymentData) {
                String amount = (String) payment.get("amount");
                if (amount != null && !amount.isEmpty()) {
                    try {
                        totalPaid = totalPaid.add(new BigDecimal(amount));
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }
            }
            if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                response.append("**Total Paid Amount: ₹").append(totalPaid).append("**\n");
            }
            
            response.append("\nSee the table below for detailed payment information.");
        }
        
        // Check for vendor data
        List<Map<String, Object>> vendorData = structuredData.stream()
            .filter(item -> "vendor".equals(item.get("type")))
            .collect(Collectors.toList());
        
        if (!vendorData.isEmpty()) {
            if (response.length() > 0) response.append("\n\n");
            
            if (lowerMessage.contains("high-risk") || lowerMessage.contains("risk")) {
                response.append("Here are the high-risk vendors:\n\n");
            } else {
                response.append("Here are the registered vendors:\n\n");
            }
            response.append("**Total Vendors: ").append(vendorData.size()).append("**\n\n");
            response.append("See the table below for detailed vendor information.");
        }
        
        // Check for approval history data
        List<Map<String, Object>> approvalData = structuredData.stream()
            .filter(item -> "approval".equals(item.get("type")))
            .collect(Collectors.toList());
        
        if (!approvalData.isEmpty()) {
            if (response.length() > 0) response.append("\n\n");
            response.append("Here is the approval history:\n\n");
            response.append("**Total Approval Records: ").append(approvalData.size()).append("**\n\n");
            response.append("See the table below for detailed approval information.");
        }
        
        // Check for approval rules data
        List<Map<String, Object>> approvalRuleData = structuredData.stream()
            .filter(item -> "approvalRule".equals(item.get("type")))
            .collect(Collectors.toList());
        
        if (!approvalRuleData.isEmpty()) {
            if (response.length() > 0) response.append("\n\n");
            response.append("Here are the approval level requirements:\n\n");
            response.append("**Total Rules: ").append(approvalRuleData.size()).append("**\n\n");
            response.append("See the table below for detailed approval rules.");
        }
        
        // Check for statistics
        List<Map<String, Object>> statsData = structuredData.stream()
            .filter(item -> "statistics".equals(item.get("type")))
            .collect(Collectors.toList());
        
        if (!statsData.isEmpty()) {
            if (response.length() > 0) response.append("\n\n");
            Map<String, Object> stats = statsData.get(0);
            
            // Check if question is specifically about vendors
            if (lowerMessage.contains("vendor") && lowerMessage.contains("how many")) {
                response.append("**Total Registered Vendors: ").append(stats.get("totalVendors")).append("**\n\n");
                response.append("See the statistics card below for more details.");
            } else {
                response.append("Here are the current statistics:\n\n");
                
                if (stats.get("totalInvoices") != null) {
                    response.append("• Total Invoices: ").append(stats.get("totalInvoices")).append("\n");
                }
                if (stats.get("pendingInvoices") != null) {
                    response.append("• Pending Invoices: ").append(stats.get("pendingInvoices")).append("\n");
                }
                if (stats.get("approvedInvoices") != null) {
                    response.append("• Approved Invoices: ").append(stats.get("approvedInvoices")).append("\n");
                }
                if (stats.get("paidInvoices") != null) {
                    response.append("• Paid Invoices: ").append(stats.get("paidInvoices")).append("\n");
                }
                if (stats.get("totalPayments") != null) {
                    response.append("• Total Payments: ").append(stats.get("totalPayments")).append("\n");
                }
                if (stats.get("totalPaidAmount") != null) {
                    response.append("• Total Paid Amount: ₹").append(stats.get("totalPaidAmount")).append("\n");
                }
                if (stats.get("pendingApprovals") != null) {
                    response.append("• Pending Approvals: ").append(stats.get("pendingApprovals")).append("\n");
                }
                if (stats.get("myTotalInvoices") != null) {
                    response.append("• Your Total Invoices: ").append(stats.get("myTotalInvoices")).append("\n");
                }
                if (stats.get("myPendingInvoices") != null) {
                    response.append("• Your Pending Invoices: ").append(stats.get("myPendingInvoices")).append("\n");
                }
                if (stats.get("myApprovedInvoices") != null) {
                    response.append("• Your Approved Invoices: ").append(stats.get("myApprovedInvoices")).append("\n");
                }
                if (stats.get("totalVendors") != null) {
                    response.append("• Total Vendors: ").append(stats.get("totalVendors")).append("\n");
                }
                if (stats.get("totalUsers") != null) {
                    response.append("• Total Users: ").append(stats.get("totalUsers")).append("\n");
                }
                if (stats.get("mostActiveUsers") != null) {
                    response.append("• Most Active Users: ").append(stats.get("mostActiveUsers")).append("\n");
                }
                response.append("\nSee the statistics cards below for more details.");
            }
        }
        
        // If no specific data found but we have data, provide generic message
        if (response.length() == 0 && !structuredData.isEmpty()) {
            response.append("I found the following information based on your query. See the details below.");
        }
        
        // If still no response but we have context data, create a basic response
        if (response.length() == 0) {
            // Try to create response from context
            String contextData = retrieveContextData(message, userRole, "");
            if (contextData != null && !contextData.isEmpty()) {
                response.append("Based on the current system data:\n\n").append(contextData);
            } else {
                // Last resort - acknowledge the question
                if (message.contains("invoice")) {
                    response.append("I can help you with invoice-related queries. ");
                    if (userRole.equals("USER")) {
                        response.append("You can view your invoices in the 'My Invoices' section.");
                    } else if (userRole.equals("MANAGER")) {
                        response.append("You can view pending approvals in the 'Invoices' section.");
                    } else if (userRole.equals("FINANCE")) {
                        response.append("You can view approved invoices ready for payment in the 'Approved Invoices' section.");
                    } else {
                        response.append("You can view all invoices in the 'Invoices' section.");
                    }
                } else if (message.contains("payment")) {
                    response.append("I can help you with payment-related queries. ");
                    if (userRole.equals("FINANCE") || userRole.equals("ADMIN")) {
                        response.append("You can view and process payments in the 'Payments' section.");
                    } else {
                        response.append("Payment information is available in the 'Payments' section.");
                    }
                } else {
                    response.append("I'm here to help. Please check the relevant sections in the application for detailed information.");
                }
            }
        }
        
        return response.length() > 0 ? response.toString() : null;
    }

    /**
     * Check if the question is from the predefined list of questions
     */
    private boolean isPredefinedQuestion(String message, String userRole) {
        String lowerMessage = message.toLowerCase().trim();
        
        // Define predefined questions for each role
        List<String> predefinedQuestions = getPredefinedQuestionsForRole(userRole);
        
        // Check if message matches any predefined question (fuzzy match)
        for (String predefinedQ : predefinedQuestions) {
            String lowerPredefined = predefinedQ.toLowerCase().trim();
            
            // Exact match
            if (lowerMessage.equals(lowerPredefined)) {
                return true;
            }
            
            // Check if message contains key keywords from predefined question
            String[] keywords = extractKeywords(lowerPredefined);
            int matchCount = 0;
            for (String keyword : keywords) {
                if (lowerMessage.contains(keyword)) {
                    matchCount++;
                }
            }
            // If 70% of keywords match, consider it a predefined question
            if (keywords.length > 0 && (matchCount * 100.0 / keywords.length) >= 70) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Get predefined questions for a specific role
     */
    private List<String> getPredefinedQuestionsForRole(String userRole) {
        List<String> questions = new ArrayList<>();
        
        if (userRole.equals("USER")) {
            questions.add("What is the status of my invoice?");
            questions.add("How do I create a new invoice?");
            questions.add("Which invoices are pending approval?");
            questions.add("Show me my invoice history");
            questions.add("What information do I need to create an invoice?");
            questions.add("How do I check if my invoice was approved?");
            questions.add("What happens after I submit an invoice?");
            questions.add("Can I edit my invoice after submission?");
            questions.add("How do I track my invoice status?");
            questions.add("What should I do if my invoice is rejected?");
            questions.add("How long does invoice approval take?");
            questions.add("What are the invoice approval levels?");
            questions.add("How do I view my invoice details?");
            questions.add("What is the difference between invoice statuses?");
            questions.add("How do I add items to my invoice?");
        } else if (userRole.equals("MANAGER")) {
            questions.add("Which invoices are pending for approval?");
            questions.add("Show me invoices requiring my approval");
            questions.add("What are the approval statistics?");
            questions.add("How do I approve an invoice?");
            questions.add("How do I reject an invoice?");
            questions.add("What should I consider before approving?");
            questions.add("Show me high-value invoices pending approval");
            questions.add("What are the approval level requirements?");
            questions.add("How do I add comments when approving?");
            questions.add("Which invoices need immediate attention?");
            questions.add("What is the approval workflow process?");
            questions.add("How do I track my approval history?");
            questions.add("What happens after I approve an invoice?");
            questions.add("Can I see who else approved this invoice?");
            questions.add("What are common rejection reasons?");
        } else if (userRole.equals("FINANCE")) {
            questions.add("Which invoices are ready for payment?");
            questions.add("Show me payment statistics");
            questions.add("How many payments were processed today?");
            questions.add("What is the total amount paid this month?");
            questions.add("How do I record a payment?");
            questions.add("Show me overdue invoices");
            questions.add("What invoices are partially paid?");
            questions.add("How do I process a full payment?");
            questions.add("What payment methods are available?");
            questions.add("Show me payment history for an invoice");
            questions.add("What is the payment workflow?");
            questions.add("How do I track payment status?");
            questions.add("Which vendors have pending payments?");
            questions.add("What is the total outstanding amount?");
            questions.add("How do I generate payment reports?");
        } else { // ADMIN
            questions.add("Show me a summary of total invoices and payments");
            questions.add("What are the system statistics?");
            questions.add("How many users are in the system?");
            questions.add("Show me all pending invoices");
            questions.add("What is the total revenue this month?");
            questions.add("How many vendors are registered?");
            questions.add("Show me system-wide approval statistics");
            questions.add("What are the most active users?");
            questions.add("How do I manage user roles?");
            questions.add("Show me invoice trends and analytics");
            questions.add("What is the system health status?");
            questions.add("How many invoices were created today?");
            questions.add("What is the average invoice amount?");
            questions.add("Show me high-risk vendors");
            questions.add("What are the system configuration options?");
        }
        
        return questions;
    }

    /**
     * Extract keywords from a question (removing common words)
     */
    private String[] extractKeywords(String question) {
        // Remove common words
        String[] commonWords = {"what", "how", "which", "show", "me", "my", "the", "a", "an", "is", "are", "do", "does", "can", "i", "you", "we", "they", "to", "for", "of", "in", "on", "at", "by", "with", "from", "and", "or", "but"};
        String[] words = question.split("\\s+");
        List<String> keywords = new ArrayList<>();
        
        for (String word : words) {
            String cleanWord = word.toLowerCase().replaceAll("[^a-z]", "");
            if (cleanWord.length() > 2 && !Arrays.asList(commonWords).contains(cleanWord)) {
                keywords.add(cleanWord);
            }
        }
        
        return keywords.toArray(new String[0]);
    }

    /**
     * Build generic help message for non-predefined questions
     */
    private String buildGenericHelpMessage(String userRole) {
        StringBuilder message = new StringBuilder();
        message.append("I'm here to help with invoice management. You can ask me about:\n\n");
        
        if (userRole.equals("USER")) {
            message.append("• Invoice status\n");
            message.append("• Creating invoices\n");
            message.append("• Invoice approval process\n");
            message.append("• Tracking your invoices\n");
        } else if (userRole.equals("MANAGER")) {
            message.append("• Pending approvals\n");
            message.append("• Approval statistics\n");
            message.append("• Approving/rejecting invoices\n");
            message.append("• Approval workflow\n");
        } else if (userRole.equals("FINANCE")) {
            message.append("• Payment information\n");
            message.append("• Payment statistics\n");
            message.append("• Processing payments\n");
            message.append("• Payment reports\n");
        } else { // ADMIN
            message.append("• Invoice status\n");
            message.append("• Pending approvals\n");
            message.append("• Payment information\n");
            message.append("• System summaries (Admin only)\n");
        }
        
        message.append("\nHow can I assist you today?");
        return message.toString();
    }
}


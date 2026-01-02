package com.svims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GenAI Service using FREE APIs (Groq/Hugging Face)
 * Provides intelligent, context-aware responses using Large Language Models
 */
@Slf4j
@Service
public class GenAIService {

    private final RestTemplate restTemplate;
    
    public GenAIService() {
        this.restTemplate = new RestTemplate();
    }

    @Value("${chatbot.provider:groq}")
    private String chatbotProvider; // groq, huggingface, or ollama

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.model:llama-3.1-8b-instant}")
    private String groqModel;

    @Value("${chatbot.enabled:true}")
    private boolean chatbotEnabled;

    /**
     * Generate AI response using FREE API (Groq) with project context
     */
    public String generateResponse(String userMessage, String userRole, String contextData) {
        if (!chatbotEnabled) {
            log.warn("Chatbot is disabled");
            return null;
        }

        // Build system prompt with project context
        String systemPrompt = buildSystemPrompt(userRole, contextData);

        log.info("Calling {} API with model: {}, role: {}", chatbotProvider, groqModel, userRole);

        try {
            if ("groq".equalsIgnoreCase(chatbotProvider)) {
                return callGroqAPI(systemPrompt, userMessage);
            } else {
                // Fallback to simple rule-based if no API configured
                return generateSimpleResponse(userMessage, userRole, contextData);
            }
        } catch (Exception e) {
            log.error("API Error: {}", e.getMessage());
            // Fallback to simple response
            return generateSimpleResponse(userMessage, userRole, contextData);
        }
    }

    /**
     * Call Groq API (FREE tier available)
     */
    private String callGroqAPI(String systemPrompt, String userMessage) {
        if (groqApiKey == null || groqApiKey.isEmpty()) {
            log.warn("Groq API key not set. Using free fallback.");
            return null;
        }

        String url = "https://api.groq.com/openai/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqModel);
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", userMessage)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 500);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                return (String) message.get("content");
            }
        }
        return null;
    }

    /**
     * Simple rule-based response (FREE - no API needed)
     */
    private String generateSimpleResponse(String userMessage, String userRole, String contextData) {
        String message = userMessage.toLowerCase();
        
        if (message.contains("invoice") && message.contains("status")) {
            if (contextData != null && !contextData.isEmpty()) {
                return "Based on your account:\n\n" + contextData + 
                       "\n\nYou can view detailed invoice information in the Invoices section.";
            }
            return "I can help you check invoice status. " +
                   "As a " + userRole + ", you can view invoices based on your role permissions. " +
                   "Would you like to see your pending or approved invoices?";
        }
        
        if (message.contains("pending") || message.contains("approval")) {
            if (userRole.equals("MANAGER") || userRole.equals("ADMIN")) {
                return "You can view pending approvals in the 'Pending Approvals' section. " +
                       "There you can approve or reject invoices based on your review.";
            }
            return "Pending approvals are handled by Managers. " +
                   "Your invoices will go through the approval workflow automatically.";
        }
        
        if (message.contains("payment") || message.contains("paid")) {
            if (userRole.equals("FINANCE") || userRole.equals("ADMIN")) {
                if (contextData != null && !contextData.isEmpty()) {
                    return "Payment Information:\n\n" + contextData + 
                           "\n\nYou can process payments from the Payments section.";
                }
                return "You can view and process payments in the Payments section. " +
                       "Approved invoices are ready for payment processing.";
            }
            return "Payment processing is handled by Finance team. " +
                   "Once your invoice is approved, it will be processed for payment.";
        }
        
        if (message.contains("help") || message.contains("what can")) {
            return buildHelpResponse(userRole);
        }
        
        return "I'm here to help with invoice management. " +
               "You can ask me about:\n" +
               "• Invoice status\n" +
               "• Pending approvals\n" +
               "• Payment information\n" +
               "• System summaries (Admin only)\n\n" +
               "How can I assist you today?";
    }

    private String buildHelpResponse(String role) {
        StringBuilder help = new StringBuilder("I can help you with:\n\n");
        
        if (role.equals("USER")) {
            help.append("• Check invoice status\n");
            help.append("• Understand invoice workflow\n");
            help.append("• Create invoices\n");
        } else if (role.equals("MANAGER")) {
            help.append("• View pending approvals\n");
            help.append("• Approve/reject invoices\n");
            help.append("• Check approval statistics\n");
        } else if (role.equals("FINANCE")) {
            help.append("• View invoices ready for payment\n");
            help.append("• Process payments\n");
            help.append("• Check payment statistics\n");
        } else if (role.equals("ADMIN")) {
            help.append("• System-wide summaries\n");
            help.append("• All features available to other roles\n");
        }
        
        return help.toString();
    }

    /**
     * Build system prompt with project-specific context
     */
    private String buildSystemPrompt(String userRole, String contextData) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an AI assistant for a Vendor & Invoice Management System (SVIMS). ");
        prompt.append("You help users understand invoice status, approvals, and payments. ");
        prompt.append("You MUST respect role-based access control and NEVER expose data beyond user permissions.\n\n");

        prompt.append("PROJECT CONTEXT:\n");
        prompt.append("- Roles: ADMIN, MANAGER, FINANCE, USER (default role)\n");
        prompt.append("- Users have only ONE role at a time\n");
        prompt.append("- Vendors are created ONLY by ADMIN\n");
        prompt.append("- Invoices are created by USER or ADMIN\n");
        prompt.append("- Invoice lifecycle: CREATED → PENDING → APPROVED → PAID\n");
        prompt.append("- Managers approve/reject invoices\n");
        prompt.append("- Finance processes payments\n");
        prompt.append("- Only ADMIN can edit invoices\n\n");

        prompt.append("CURRENT USER ROLE: ").append(userRole).append("\n");
        prompt.append("ROLE PERMISSIONS:\n");

        switch (userRole) {
            case "USER":
                prompt.append("- Can view their own invoices (PENDING, APPROVED status)\n");
                prompt.append("- Can create invoices\n");
                prompt.append("- CANNOT see all invoices or payment details\n");
                break;
            case "MANAGER":
                prompt.append("- Can view pending approvals\n");
                prompt.append("- Can approve/reject invoices\n");
                prompt.append("- Can view approval statistics\n");
                prompt.append("- CANNOT process payments\n");
                break;
            case "FINANCE":
                prompt.append("- Can view approved invoices ready for payment\n");
                prompt.append("- Can process payments\n");
                prompt.append("- Can view payment statistics\n");
                prompt.append("- CANNOT approve invoices\n");
                break;
            case "ADMIN":
                prompt.append("- Full access to all features\n");
                prompt.append("- Can view system-wide summaries\n");
                prompt.append("- Can manage users and vendors\n");
                break;
        }

        if (contextData != null && !contextData.isEmpty()) {
            prompt.append("\nCURRENT DATA CONTEXT (USE THIS DATA IN YOUR RESPONSE):\n").append(contextData).append("\n");
            prompt.append("IMPORTANT: The data above is REAL and CURRENT. Always reference this actual data in your response.\n");
            prompt.append("If the user asks about statistics, counts, or numbers, use the exact numbers from the data above.\n");
        }

        prompt.append("\nINSTRUCTIONS:\n");
        prompt.append("- Answer questions clearly and concisely\n");
        prompt.append("- ALWAYS use the actual data provided in CURRENT DATA CONTEXT when available\n");
        prompt.append("- When data is provided, include specific numbers, counts, and details in your response\n");
        prompt.append("- Use business-friendly language\n");
        prompt.append("- NEVER expose data beyond the user's role permissions\n");
        prompt.append("- If asked about data you don't have access to, politely decline\n");
        prompt.append("- Format responses with bullet points when listing items\n");
        prompt.append("- Be helpful and professional\n");
        prompt.append("- If data shows 0 or empty, mention that clearly (e.g., 'Currently, there are no pending invoices')\n");

        return prompt.toString();
    }

    /**
     * Check if chatbot is enabled
     */
    public boolean isGenAIEnabled() {
        return chatbotEnabled;
    }
}


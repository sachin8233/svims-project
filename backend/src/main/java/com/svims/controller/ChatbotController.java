package com.svims.controller;

import com.svims.dto.ChatbotRequest;
import com.svims.dto.ChatbotResponse;
import com.svims.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

/**
 * Chatbot Controller
 * REST API for AI-powered chatbot interactions
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "AI-powered chatbot for invoice management assistance")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI assistant", 
               description = "Send a message to the AI chatbot. Responses are role-aware and filtered based on user permissions.")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'USER')")
    public ResponseEntity<ChatbotResponse> chat(
            @RequestBody ChatbotRequest request,
            Authentication authentication) {
        
        // Extract user information from authentication
        String username = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");
        
        // Set user context in request
        request.setUsername(username);
        request.setRole(role);
        
        // Process message with role-aware logic
        ChatbotResponse response = chatbotService.processMessage(request);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/help")
    @Operation(summary = "Get chatbot help", 
               description = "Get help information about what the chatbot can do based on your role")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'USER')")
    public ResponseEntity<ChatbotResponse> getHelp(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER");
        
        ChatbotRequest helpRequest = new ChatbotRequest();
        helpRequest.setMessage("help");
        helpRequest.setUsername(authentication.getName());
        helpRequest.setRole(role);
        
        ChatbotResponse response = chatbotService.processMessage(helpRequest);
        return ResponseEntity.ok(response);
    }
}


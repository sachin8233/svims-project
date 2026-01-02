package com.svims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chatbot Request DTO
 * Contains the user's question/message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {
    private String message;
    private String username; // Will be set from authentication
    private String role; // Will be set from authentication
}


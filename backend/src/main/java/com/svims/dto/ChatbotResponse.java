package com.svims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Chatbot Response DTO
 * Contains the chatbot's response with any relevant data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {
    private String response;
    private String role; // User's role for context
    private List<Map<String, Object>> data; // Optional structured data
    private String suggestion; // Optional suggestion or follow-up
}


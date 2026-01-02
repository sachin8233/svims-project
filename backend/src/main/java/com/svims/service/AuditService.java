package com.svims.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.svims.entity.AuditLog;
import com.svims.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Service for audit logging
 * Logs all actions for audit trail
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Log an action to audit trail
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String userName, String action, String entityType, Long entityId,
                         Object oldValue, Object newValue, String description, HttpServletRequest request) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserName(userName);
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setDescription(description);
            
            // Try to serialize old value, but don't fail if it fails
            if (oldValue != null) {
                try {
                    auditLog.setOldValue(objectMapper.writeValueAsString(oldValue));
                } catch (Exception e) {
                    log.warn("Failed to serialize oldValue for audit log: {}", e.getMessage());
                    auditLog.setOldValue("Serialization failed: " + oldValue.getClass().getSimpleName());
                }
            }
            
            // Try to serialize new value, but don't fail if it fails
            if (newValue != null) {
                try {
                    auditLog.setNewValue(objectMapper.writeValueAsString(newValue));
                } catch (Exception e) {
                    log.warn("Failed to serialize newValue for audit log: {}", e.getMessage());
                    auditLog.setNewValue("Serialization failed: " + newValue.getClass().getSimpleName());
                }
            }
            
            if (request != null) {
                auditLog.setIpAddress(getClientIpAddress(request));
            }
            
            AuditLog saved = auditLogRepository.save(auditLog);
            log.debug("Audit log created successfully: ID={}, User={}, Action={}, Entity={}", 
                    saved.getId(), userName, action, entityType);
        } catch (Exception e) {
            // Log error but don't fail the main transaction
            log.error("Failed to create audit log for user: {}, action: {}, entity: {}. Error: {}", 
                    userName, action, entityType, e.getMessage(), e);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}


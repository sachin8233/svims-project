package com.svims.service;

import com.svims.dto.AuditLogDTO;
import com.svims.entity.AuditLog;
import com.svims.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for retrieving audit logs
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Get all audit logs with pagination
     */
    public Page<AuditLogDTO> getAllAuditLogs(Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findAll(
            PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp")
            )
        );
        return auditLogs.map(this::convertToDTO);
    }

    /**
     * Get audit logs by user
     */
    public List<AuditLogDTO> getAuditLogsByUser(String userName) {
        return auditLogRepository.findByUserName(userName).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get audit logs by entity
     */
    public List<AuditLogDTO> getAuditLogsByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get audit logs by date range
     */
    public List<AuditLogDTO> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findAuditLogsByDateRange(startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all audit logs (for simple list view)
     */
    public List<AuditLogDTO> getAllAuditLogs() {
        return auditLogRepository.findAll(
            Sort.by(Sort.Direction.DESC, "timestamp")
        ).stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
    }

    private AuditLogDTO convertToDTO(AuditLog auditLog) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(auditLog.getId());
        dto.setUserName(auditLog.getUserName());
        dto.setAction(auditLog.getAction());
        dto.setEntityType(auditLog.getEntityType());
        dto.setEntityId(auditLog.getEntityId());
        dto.setOldValue(auditLog.getOldValue());
        dto.setNewValue(auditLog.getNewValue());
        dto.setIpAddress(auditLog.getIpAddress());
        dto.setTimestamp(auditLog.getTimestamp());
        dto.setDescription(auditLog.getDescription());
        return dto;
    }
}


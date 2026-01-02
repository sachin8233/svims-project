package com.svims.service;

import com.svims.dto.ApprovalRuleDTO;
import com.svims.entity.ApprovalRule;
import com.svims.repository.ApprovalRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for ApprovalRule operations
 */
@Service
@RequiredArgsConstructor
public class ApprovalRuleService {

    private final ApprovalRuleRepository approvalRuleRepository;

    public List<ApprovalRuleDTO> getAllApprovalRules() {
        return approvalRuleRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ApprovalRuleDTO> getActiveApprovalRules() {
        return approvalRuleRepository.findByIsActiveTrueOrderByPriorityAsc().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ApprovalRuleDTO getApprovalRuleById(Long id) {
        ApprovalRule rule = approvalRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval rule not found with id: " + id));
        return convertToDTO(rule);
    }

    @Transactional
    public ApprovalRuleDTO createApprovalRule(ApprovalRuleDTO ruleDTO) {
        // Validate amount range
        if (ruleDTO.getMinAmount().compareTo(ruleDTO.getMaxAmount()) >= 0) {
            throw new RuntimeException("Minimum amount must be less than maximum amount");
        }

        // Check for overlapping ranges
        validateNoOverlappingRanges(ruleDTO.getMinAmount(), ruleDTO.getMaxAmount(), null);

        ApprovalRule rule = convertToEntity(ruleDTO);
        rule = approvalRuleRepository.save(rule);
        return convertToDTO(rule);
    }

    @Transactional
    public ApprovalRuleDTO updateApprovalRule(Long id, ApprovalRuleDTO ruleDTO) {
        ApprovalRule rule = approvalRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval rule not found with id: " + id));

        // Validate amount range
        if (ruleDTO.getMinAmount().compareTo(ruleDTO.getMaxAmount()) >= 0) {
            throw new RuntimeException("Minimum amount must be less than maximum amount");
        }

        // Check for overlapping ranges (excluding current rule)
        validateNoOverlappingRanges(ruleDTO.getMinAmount(), ruleDTO.getMaxAmount(), id);

        rule.setMinAmount(ruleDTO.getMinAmount());
        rule.setMaxAmount(ruleDTO.getMaxAmount());
        rule.setApprovalLevels(ruleDTO.getApprovalLevels());
        rule.setRequiredRoles(ruleDTO.getRequiredRoles());
        rule.setIsActive(ruleDTO.getIsActive());
        rule.setPriority(ruleDTO.getPriority());

        rule = approvalRuleRepository.save(rule);
        return convertToDTO(rule);
    }

    @Transactional
    public void deleteApprovalRule(Long id) {
        if (!approvalRuleRepository.existsById(id)) {
            throw new RuntimeException("Approval rule not found with id: " + id);
        }
        approvalRuleRepository.deleteById(id);
    }

    @Transactional
    public ApprovalRuleDTO toggleApprovalRuleStatus(Long id) {
        ApprovalRule rule = approvalRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Approval rule not found with id: " + id));
        rule.setIsActive(!rule.getIsActive());
        rule = approvalRuleRepository.save(rule);
        return convertToDTO(rule);
    }

    private void validateNoOverlappingRanges(BigDecimal minAmount, BigDecimal maxAmount, Long excludeId) {
        List<ApprovalRule> existingRules = approvalRuleRepository.findAll();
        
        for (ApprovalRule existing : existingRules) {
            if (excludeId != null && existing.getId().equals(excludeId)) {
                continue;
            }
            
            // Check if ranges overlap
            boolean overlaps = (minAmount.compareTo(existing.getMaxAmount()) < 0) &&
                              (maxAmount.compareTo(existing.getMinAmount()) > 0);
            
            if (overlaps) {
                throw new RuntimeException(
                    String.format("Amount range overlaps with existing rule (ID: %d, Range: %s - %s)",
                        existing.getId(), existing.getMinAmount(), existing.getMaxAmount()));
            }
        }
    }

    private ApprovalRuleDTO convertToDTO(ApprovalRule rule) {
        ApprovalRuleDTO dto = new ApprovalRuleDTO();
        dto.setId(rule.getId());
        dto.setMinAmount(rule.getMinAmount());
        dto.setMaxAmount(rule.getMaxAmount());
        dto.setApprovalLevels(rule.getApprovalLevels());
        dto.setRequiredRoles(rule.getRequiredRoles());
        dto.setIsActive(rule.getIsActive());
        dto.setPriority(rule.getPriority());
        dto.setCreatedAt(rule.getCreatedAt());
        dto.setUpdatedAt(rule.getUpdatedAt());
        return dto;
    }

    private ApprovalRule convertToEntity(ApprovalRuleDTO dto) {
        ApprovalRule rule = new ApprovalRule();
        rule.setMinAmount(dto.getMinAmount());
        rule.setMaxAmount(dto.getMaxAmount());
        rule.setApprovalLevels(dto.getApprovalLevels());
        rule.setRequiredRoles(dto.getRequiredRoles());
        rule.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        rule.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        return rule;
    }
}



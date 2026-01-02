package com.svims.controller;

import com.svims.dto.ApprovalRuleDTO;
import com.svims.service.ApprovalRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Approval Rule Controller
 * REST API for Approval Rule CRUD operations (Admin only)
 */
@RestController
@RequestMapping("/api/approval-rules")
@RequiredArgsConstructor
@Tag(name = "Approval Rules", description = "Approval rule management APIs")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class ApprovalRuleController {

    private final ApprovalRuleService approvalRuleService;

    @GetMapping
    @Operation(summary = "Get all approval rules", description = "Retrieve list of all approval rules (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ApprovalRuleDTO>> getAllApprovalRules() {
        return ResponseEntity.ok(approvalRuleService.getAllApprovalRules());
    }

    @GetMapping("/active")
    @Operation(summary = "Get active approval rules", description = "Retrieve list of active approval rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<List<ApprovalRuleDTO>> getActiveApprovalRules() {
        return ResponseEntity.ok(approvalRuleService.getActiveApprovalRules());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get approval rule by ID", description = "Retrieve approval rule details by ID (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApprovalRuleDTO> getApprovalRuleById(@PathVariable Long id) {
        return ResponseEntity.ok(approvalRuleService.getApprovalRuleById(id));
    }

    @PostMapping
    @Operation(summary = "Create approval rule", description = "Create a new approval rule (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createApprovalRule(@Valid @RequestBody ApprovalRuleDTO ruleDTO) {
        try {
            return new ResponseEntity<>(approvalRuleService.createApprovalRule(ruleDTO), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create approval rule: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update approval rule", description = "Update existing approval rule (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateApprovalRule(@PathVariable Long id, 
                                                @Valid @RequestBody ApprovalRuleDTO ruleDTO) {
        try {
            return ResponseEntity.ok(approvalRuleService.updateApprovalRule(id, ruleDTO));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update approval rule: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete approval rule", description = "Delete an approval rule by ID (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteApprovalRule(@PathVariable Long id) {
        try {
            approvalRuleService.deleteApprovalRule(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle approval rule status", description = "Activate or deactivate an approval rule (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApprovalRuleDTO> toggleApprovalRuleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(approvalRuleService.toggleApprovalRuleStatus(id));
    }
}



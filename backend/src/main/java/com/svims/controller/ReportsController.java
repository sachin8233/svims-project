package com.svims.controller;

import com.svims.dto.ReportsDTO;
import com.svims.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Reports Controller
 * REST API for Reports and Analytics (Admin only)
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Reports and analytics APIs")
@SecurityRequirement(name = "bearerAuth")
@CrossOrigin(origins = "*")
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard reports", description = "Retrieve comprehensive dashboard reports with statistics (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportsDTO> getDashboardReports() {
        return ResponseEntity.ok(reportsService.getDashboardReports());
    }
}



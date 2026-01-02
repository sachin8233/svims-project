package com.svims.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user role (single role only)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRolesRequest {
    @NotBlank(message = "Role is required")
    private String role;
}


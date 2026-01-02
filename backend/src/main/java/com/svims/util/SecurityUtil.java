package com.svims.util;

import com.svims.entity.User;
import com.svims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Utility class for security-related operations
 * Provides methods to get current user and check roles
 */
@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    /**
     * Get the current authenticated username
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Get the current authenticated user entity
     */
    public User getCurrentUser() {
        String username = getCurrentUsername();
        if (username != null) {
            return userRepository.findByUsername(username).orElse(null);
        }
        return null;
    }

    /**
     * Get current user's roles
     */
    public Collection<String> getCurrentUserRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(authority -> authority.replace("ROLE_", ""))
                    .collect(Collectors.toList());
        }
        return null;
    }

    /**
     * Check if current user has a specific role
     */
    public boolean hasRole(String role) {
        Collection<String> roles = getCurrentUserRoles();
        if (roles != null) {
            return roles.contains(role.toUpperCase());
        }
        return false;
    }

    /**
     * Check if current user is ADMIN
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Check if current user is MANAGER
     */
    public boolean isManager() {
        return hasRole("MANAGER");
    }

    /**
     * Check if current user is FINANCE
     */
    public boolean isFinance() {
        return hasRole("FINANCE");
    }

    /**
     * Check if current user is USER (regular user)
     */
    public boolean isUser() {
        return hasRole("USER");
    }
}


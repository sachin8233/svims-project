package com.svims.service;

import com.svims.dto.UpdateUserRolesRequest;
import com.svims.dto.UserDTO;
import com.svims.entity.Role;
import com.svims.entity.User;
import com.svims.repository.RoleRepository;
import com.svims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for user management operations
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToDTO(user);
    }

    @Transactional
    public UserDTO updateUserRoles(Long userId, UpdateUserRolesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Validate and convert role name to Role entity (single role only)
        try {
            Role.RoleName roleNameEnum = Role.RoleName.valueOf(request.getRole());
            Role role = roleRepository.findByName(roleNameEnum)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRole()));
            
            // Set only one role
            Set<Role> roles = new HashSet<>();
            roles.add(role);
            user.setRoles(roles);
            
            User updatedUser = userRepository.save(user);
            return convertToDTO(updatedUser);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role name: " + request.getRole());
        }
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
    }

    private UserDTO convertToDTO(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getIsActive(),
                roleNames
        );
    }
}


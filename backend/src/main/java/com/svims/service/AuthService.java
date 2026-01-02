package com.svims.service;

import com.svims.dto.JwtResponse;
import com.svims.dto.LoginRequest;
import com.svims.dto.RegisterRequest;

import com.svims.entity.Role;
import com.svims.entity.User;
import com.svims.repository.RoleRepository;
import com.svims.repository.UserRepository;
import com.svims.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for authentication operations
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public JwtResponse authenticate(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenUtil.generateToken((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal());

            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());

            return new JwtResponse(jwt, "Bearer", user.getId(), user.getUsername(), user.getEmail(), roles);
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid username or password", e);
        }
    }

    public JwtResponse register(RegisterRequest registerRequest) {
        // Validate all fields before proceeding
        validateRegistrationRequest(registerRequest);

        // All validations passed, now create and save user
        User user = new User();
        user.setUsername(registerRequest.getUsername().trim());
        user.setEmail(registerRequest.getEmail().trim().toLowerCase());
        // user.setPassword(passwordEncoder.encode(registerRequest.getPassword())); // Commented out - storing plain text
        user.setPassword(registerRequest.getPassword()); // Storing password directly without encoding
        user.setIsActive(true);

        // Assign default role (USER) to new users - Admin can change this later from UI
        Set<Role> roles = new HashSet<>();
        Role defaultRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default role (USER) not found"));
        roles.add(defaultRole);
        user.setRoles(roles);

        // Save to database only after all validations pass
        User savedUser = userRepository.save(user);

        // Automatically authenticate the newly registered user
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(registerRequest.getUsername(), registerRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenUtil.generateToken((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal());

        List<String> roleNames = savedUser.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        return new JwtResponse(jwt, "Bearer", savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), roleNames);
    }

    private void validateRegistrationRequest(RegisterRequest registerRequest) {
        // Validate username
        if (registerRequest.getUsername() == null || registerRequest.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        String username = registerRequest.getUsername().trim();
        if (username.length() < 3 || username.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
        if (!Pattern.matches("^[a-zA-Z0-9_]+$", username)) {
            throw new IllegalArgumentException("Username can only contain letters, numbers, and underscores");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Validate email
        if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        String email = registerRequest.getEmail().trim().toLowerCase();
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (!Pattern.matches(emailRegex, email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Validate password
        if (registerRequest.getPassword() == null || registerRequest.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        String password = registerRequest.getPassword();
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
        if (password.length() > 100) {
            throw new IllegalArgumentException("Password must be less than 100 characters");
        }
    }

    public User createUser(String username, String email, String password, List<String> roleNames) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        // user.setPassword(passwordEncoder.encode(password)); // Commented out - storing plain text
        user.setPassword(password); // Storing password directly without encoding
        user.setIsActive(true);

        Set<Role> roles = roleNames.stream()
                .map(roleName -> roleRepository.findByName(Role.RoleName.valueOf(roleName))
                        .orElseThrow(() -> new RuntimeException("Role not found: " + roleName)))
                .collect(Collectors.toSet());
        user.setRoles(roles);

        return userRepository.save(user);
    }
}


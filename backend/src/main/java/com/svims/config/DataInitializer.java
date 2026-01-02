package com.svims.config;

import com.svims.entity.Role;
import com.svims.entity.User;
import com.svims.repository.RoleRepository;
import com.svims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Data Initializer
 * Creates default roles and admin user on application startup
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // Create roles if they don't exist
        if (!roleRepository.findByName(Role.RoleName.ROLE_ADMIN).isPresent()) {
            Role adminRole = new Role();
            adminRole.setName(Role.RoleName.ROLE_ADMIN);
            roleRepository.save(adminRole);
        }

        if (!roleRepository.findByName(Role.RoleName.ROLE_MANAGER).isPresent()) {
            Role managerRole = new Role();
            managerRole.setName(Role.RoleName.ROLE_MANAGER);
            roleRepository.save(managerRole);
        }

        if (!roleRepository.findByName(Role.RoleName.ROLE_FINANCE).isPresent()) {
            Role financeRole = new Role();
            financeRole.setName(Role.RoleName.ROLE_FINANCE);
            roleRepository.save(financeRole);
        }

        if (!roleRepository.findByName(Role.RoleName.ROLE_USER).isPresent()) {
            Role userRole = new Role();
            userRole.setName(Role.RoleName.ROLE_USER);
            roleRepository.save(userRole);
        }

        // Create or update admin user
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@svims.com");
        }
        // Always update password to ensure it's correct
        // admin.setPassword(passwordEncoder.encode("admin@123")); // Commented out - storing plain text
        admin.setPassword("admin@123"); // Storing password directly without encoding
        admin.setIsActive(true);

        Set<Role> roles = new HashSet<>();
        roles.add(roleRepository.findByName(Role.RoleName.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin role not found")));
        admin.setRoles(roles);

        userRepository.save(admin);
    }
}


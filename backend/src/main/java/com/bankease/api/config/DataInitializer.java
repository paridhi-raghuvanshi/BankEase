package com.bankease.api.config;

import com.bankease.api.entity.Role;
import com.bankease.api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes default roles in the database if they don't exist.
 * Runs on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        initRoles();
    }

    private void initRoles() {
        for (Role.RoleName roleName : Role.RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = Role.builder()
                        .name(roleName)
                        .description(getDescription(roleName))
                        .build();
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
        }
    }

    private String getDescription(Role.RoleName roleName) {
        return switch (roleName) {
            case ROLE_CUSTOMER -> "Standard banking customer with account access";
            case ROLE_BANKER -> "Bank employee with customer management privileges";
            case ROLE_ADMIN -> "System administrator with full access";
        };
    }
}

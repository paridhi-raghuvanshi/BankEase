package com.bankease.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * JPA Auditing configuration for the BankEase application.
 *
 * <p>Enables Spring Data JPA auditing so that entity fields annotated with
 * {@code @CreatedBy}, {@code @LastModifiedBy}, {@code @CreatedDate}, and
 * {@code @LastModifiedDate} are automatically populated on persist and merge.</p>
 *
 * <p>The {@link AuditorAware} bean resolves the "current user" from the
 * Spring Security {@link SecurityContextHolder}. For unauthenticated operations
 * (e.g., self-registration) the auditor falls back to {@code "SYSTEM"}.</p>
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaConfig {

    /** Fallback auditor name used when no authenticated principal is available. */
    private static final String SYSTEM_AUDITOR = "SYSTEM";

    /**
     * Provides the email address of the currently authenticated user as the JPA auditor.
     *
     * <p>Resolution order:</p>
     * <ol>
     *   <li>If the {@link SecurityContextHolder} holds a {@link UserDetails} principal,
     *       return {@link UserDetails#getUsername()} (which in BankEase is the user's email).</li>
     *   <li>If the principal is a plain {@link String} (e.g., set manually in tests), use it.</li>
     *   <li>If there is no authenticated context, return {@code "SYSTEM"}.</li>
     * </ol>
     *
     * @return an {@link AuditorAware} implementation that resolves to an {@link Optional} email
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of(SYSTEM_AUDITOR);
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails userDetails) {
                return Optional.of(userDetails.getUsername());
            }

            if (principal instanceof String principalStr) {
                // Handles the "anonymousUser" string Spring Security sets for unauthenticated requests
                if ("anonymousUser".equalsIgnoreCase(principalStr)) {
                    return Optional.of(SYSTEM_AUDITOR);
                }
                return Optional.of(principalStr);
            }

            return Optional.of(SYSTEM_AUDITOR);
        };
    }
}

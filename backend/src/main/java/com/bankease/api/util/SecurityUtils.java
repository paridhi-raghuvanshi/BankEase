package com.bankease.api.util;

import com.bankease.api.model.User;
import com.bankease.api.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Utility component for Spring Security context operations.
 *
 * <p>Provides helper methods to resolve the currently authenticated {@link User}
 * entity from the {@link SecurityContextHolder}, avoiding repetition across controllers.</p>
 */
@Slf4j
@Component
public class SecurityUtils {

    /**
     * Resolves the currently authenticated {@link User} entity from the security context.
     *
     * <p>Extracts the principal name (email) from the current {@link Authentication},
     * then performs a database lookup via {@link UserRepository}.</p>
     *
     * @param userRepository the repository used to load the user by email
     * @return the authenticated {@link User}
     * @throws IllegalStateException    if no authenticated principal is present
     * @throws jakarta.persistence.EntityNotFoundException if the user is not found in the database
     */
    public User getCurrentUser(UserRepository userRepository) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Attempted to retrieve current user with no active authentication in SecurityContext.");
            throw new IllegalStateException("No authenticated user found in the security context.");
        }

        String email = extractEmail(authentication);
        log.debug("Resolving current user for principal email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Authenticated principal '{}' not found in the database.", email);
                    return new jakarta.persistence.EntityNotFoundException(
                            "Authenticated user not found for email: " + email);
                });
    }

    /**
     * Resolves the ID of the currently authenticated user.
     *
     * @param userRepository the repository used to load the user by email
     * @return the {@code Long} ID of the authenticated user
     */
    public Long getCurrentUserId(UserRepository userRepository) {
        return getCurrentUser(userRepository).getId();
    }

    /**
     * Extracts the email (username) from a Spring Security {@link Authentication} principal.
     *
     * <p>Handles both {@link UserDetails} principals and raw {@link String} principal names.</p>
     *
     * @param authentication the current authentication object
     * @return the email string
     * @throws IllegalStateException if the principal type is unrecognised
     */
    private String extractEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof String email) {
            return email;
        }

        log.error("Unrecognised principal type: {}", principal.getClass().getName());
        throw new IllegalStateException(
                "Cannot extract email from principal of type: " + principal.getClass().getName());
    }
}

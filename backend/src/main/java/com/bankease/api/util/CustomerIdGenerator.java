package com.bankease.api.util;

import com.bankease.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Utility component for generating unique customer IDs.
 * <p>
 * Customer IDs follow the format {@code CUST} + 8 random digits, e.g. {@code CUST12345678}.
 * Uniqueness is verified against the database before the ID is returned.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class CustomerIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(CustomerIdGenerator.class);

    private static final String PREFIX = "CUST";
    private static final int DIGIT_COUNT = 8;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepository;

    /**
     * Generates a unique customer ID of the form {@code CUST########}.
     *
     * @return a 12-character unique customer ID string
     * @throws IllegalStateException if a unique ID cannot be generated within {@value MAX_ATTEMPTS} attempts
     */
    public String generate() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String candidate = buildCustomerId();
            if (!userRepository.existsByCustomerId(candidate)) {
                log.debug("Generated unique customer ID '{}' on attempt {}", candidate, attempt);
                return candidate;
            }
            log.warn("Customer ID collision for '{}' on attempt {}, retrying...", candidate, attempt);
        }
        throw new IllegalStateException(
                "Failed to generate a unique customer ID after " + MAX_ATTEMPTS + " attempts");
    }

    /**
     * Builds a candidate customer ID by appending {@value DIGIT_COUNT} random digits to {@value PREFIX}.
     *
     * @return candidate customer ID
     */
    private String buildCustomerId() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < DIGIT_COUNT; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}

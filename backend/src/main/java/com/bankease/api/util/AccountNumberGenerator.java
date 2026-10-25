package com.bankease.api.util;

import com.bankease.api.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Utility component for generating unique 16-digit account numbers.
 * <p>
 * Account numbers are prefixed with '4' (denoting a savings account) and the
 * remaining 15 digits are generated using {@link SecureRandom}. Uniqueness is
 * guaranteed by verifying each candidate against the database before returning it.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private static final Logger log = LoggerFactory.getLogger(AccountNumberGenerator.class);

    private static final int ACCOUNT_NUMBER_LENGTH = 16;
    private static final String SAVINGS_PREFIX = "4";
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom secureRandom = new SecureRandom();
    private final AccountRepository accountRepository;

    /**
     * Generates a unique 16-digit account number prefixed with '4'.
     *
     * @return a unique 16-character account number string
     * @throws IllegalStateException if a unique number cannot be generated within {@value MAX_ATTEMPTS} attempts
     */
    public String generate() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String candidate = buildAccountNumber();
            if (!accountRepository.existsByAccountNumber(candidate)) {
                log.debug("Generated unique account number on attempt {}", attempt);
                return candidate;
            }
            log.warn("Account number collision on attempt {}, retrying...", attempt);
        }
        throw new IllegalStateException(
                "Failed to generate a unique account number after " + MAX_ATTEMPTS + " attempts");
    }

    /**
     * Builds a 16-digit account number string prefixed with '4'.
     *
     * @return candidate account number
     */
    private String buildAccountNumber() {
        // Remaining 15 digits after the prefix
        int remainingDigits = ACCOUNT_NUMBER_LENGTH - SAVINGS_PREFIX.length();
        StringBuilder sb = new StringBuilder(SAVINGS_PREFIX);
        for (int i = 0; i < remainingDigits; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}

package com.bankease.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested entity (user, account, loan, etc.) cannot be found.
 * Maps to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends BankEaseException {

    /**
     * @param message a descriptive message such as "Account not found with id: 42"
     */
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    /**
     * Convenience constructor that formats a standard "resource not found" message.
     *
     * @param resourceName the entity type (e.g. "Account", "User")
     * @param fieldName    the field used to look up the entity (e.g. "id", "accountNumber")
     * @param fieldValue   the value that was searched for
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
                HttpStatus.NOT_FOUND);
    }
}

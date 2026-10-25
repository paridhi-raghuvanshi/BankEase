package com.bankease.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all BankEase domain-specific errors.
 * Carries an {@link HttpStatus} so that the global handler can map it
 * directly to an HTTP response code without extra branching.
 */
public class BankEaseException extends RuntimeException {

    private final HttpStatus status;

    /**
     * @param message human-readable error description
     * @param status  the HTTP status code to return to the client
     */
    public BankEaseException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    /**
     * @param message human-readable error description
     * @param status  the HTTP status code to return to the client
     * @param cause   the underlying cause
     */
    public BankEaseException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Returns the HTTP status associated with this exception.
     *
     * @return the {@link HttpStatus}
     */
    public HttpStatus getStatus() {
        return status;
    }
}

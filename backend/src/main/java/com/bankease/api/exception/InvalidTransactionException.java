package com.bankease.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a transaction request violates business rules
 * (e.g., same source and destination account, daily limit exceeded,
 * invalid transfer mode for the amount, etc.).
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidTransactionException extends BankEaseException {

    /**
     * @param message a descriptive reason why the transaction is invalid
     */
    public InvalidTransactionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    /**
     * @param message a descriptive reason why the transaction is invalid
     * @param cause   the underlying cause
     */
    public InvalidTransactionException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_REQUEST, cause);
    }
}

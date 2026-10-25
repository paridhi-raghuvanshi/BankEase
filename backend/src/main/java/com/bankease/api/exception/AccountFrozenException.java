package com.bankease.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation is attempted on a frozen (suspended) bank account.
 * Maps to HTTP 403 Forbidden.
 */
public class AccountFrozenException extends BankEaseException {

    /**
     * @param accountNumber the account number that is frozen
     */
    public AccountFrozenException(String accountNumber) {
        super(String.format("Account %s is currently frozen. Please contact customer support.", accountNumber),
                HttpStatus.FORBIDDEN);
    }

    /**
     * @param message a custom descriptive message
     */
    public AccountFrozenException(String accountNumber, String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}

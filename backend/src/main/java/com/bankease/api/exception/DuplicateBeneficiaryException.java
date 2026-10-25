package com.bankease.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a user attempts to add a beneficiary that already exists
 * (matched by account number and IFSC code).
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateBeneficiaryException extends BankEaseException {

    /**
     * @param accountNumber the duplicate account number
     * @param ifscCode      the IFSC code of the duplicate beneficiary
     */
    public DuplicateBeneficiaryException(String accountNumber, String ifscCode) {
        super(String.format(
                "A beneficiary with account number %s and IFSC %s already exists.",
                accountNumber, ifscCode),
                HttpStatus.CONFLICT);
    }

    /**
     * @param message a custom descriptive message
     */
    public DuplicateBeneficiaryException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

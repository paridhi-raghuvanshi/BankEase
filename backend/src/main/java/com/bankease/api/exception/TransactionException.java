package com.bankease.api.exception;

import org.springframework.http.HttpStatus;

public class TransactionException extends BankEaseException {
    public TransactionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}

package com.bankease.api.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends BankEaseException {
    public AccountLockedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}

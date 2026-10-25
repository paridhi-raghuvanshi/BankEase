package com.bankease.api.exception;

import org.springframework.http.HttpStatus;

public class BadCredentialsException extends BankEaseException {
    public BadCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

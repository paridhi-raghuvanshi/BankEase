package com.bankease.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends BankEaseException {
    public InvalidTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

package com.bankease.api.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Thrown when a debit or transfer cannot proceed because the account's
 * available balance is lower than the requested amount.
 * Maps to HTTP 422 Unprocessable Entity.
 */
public class InsufficientFundsException extends BankEaseException {

    private final String accountNumber;
    private final BigDecimal requestedAmount;
    private final BigDecimal availableBalance;

    /**
     * @param accountNumber    the account that has insufficient funds
     * @param requestedAmount  the amount that was attempted
     * @param availableBalance the current available balance
     */
    public InsufficientFundsException(String accountNumber, BigDecimal requestedAmount, BigDecimal availableBalance) {
        super(String.format(
                "Insufficient funds in account %s. Requested: ₹%s, Available: ₹%s",
                accountNumber, requestedAmount, availableBalance),
                HttpStatus.UNPROCESSABLE_ENTITY);
        this.accountNumber = accountNumber;
        this.requestedAmount = requestedAmount;
        this.availableBalance = availableBalance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }
}

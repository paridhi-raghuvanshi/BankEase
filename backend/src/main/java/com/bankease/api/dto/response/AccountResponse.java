package com.bankease.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO representing a bank account summary.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private String ifscCode;

    /** Account type (e.g. SAVINGS, CURRENT). */
    private String accountType;

    private BigDecimal balance;
    private BigDecimal availableBalance;

    /** ISO 4217 currency code, e.g. "INR". */
    private String currency;

    /** Account status (e.g. ACTIVE, FROZEN, CLOSED). */
    private String status;

    private BigDecimal dailyTransferLimit;
    private LocalDateTime createdAt;
}

package com.bankease.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for a single financial transaction record.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;

    /** Unique reference number shown to the customer (e.g. TXN20241025XXXXXX). */
    private String referenceNumber;

    /** Transaction type (e.g. DEBIT, CREDIT). */
    private String type;

    /** Transfer mode (e.g. NEFT, RTGS, IMPS). */
    private String mode;

    private BigDecimal amount;
    private BigDecimal charges;

    /** ISO 4217 currency code. */
    private String currency;

    private String description;

    /** Processing status (e.g. COMPLETED, PENDING, FAILED). */
    private String status;

    private String beneficiaryName;
    private String beneficiaryAccountNumber;
    private String beneficiaryIfsc;

    /** Account balance immediately after this transaction was applied. */
    private BigDecimal balanceAfterTransaction;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}

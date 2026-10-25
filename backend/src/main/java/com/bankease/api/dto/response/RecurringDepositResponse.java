package com.bankease.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for a Recurring Deposit account.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringDepositResponse {

    private Long id;

    /** Unique RD reference number (e.g. RD20241025XXXXXX). */
    private String rdNumber;

    private BigDecimal monthlyInstallment;
    private BigDecimal interestRate;
    private Integer tenureMonths;

    /** Number of installments successfully collected so far. */
    private Integer installmentsPaid;

    /** Sum of all installments deposited to date. */
    private BigDecimal totalDeposited;

    /** Projected maturity amount at the end of tenure. */
    private BigDecimal maturityAmount;

    private LocalDate maturityDate;
    private LocalDate nextInstallmentDate;

    /** RD status (e.g. ACTIVE, MATURED, CLOSED, DEFAULTED). */
    private String status;
}

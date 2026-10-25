package com.bankease.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for a loan account.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanResponse {

    private Long id;

    /** Unique loan reference number (e.g. LN20241025XXXXXX). */
    private String loanNumber;

    /** Loan type (e.g. HOME, PERSONAL, VEHICLE, EDUCATION). */
    private String loanType;

    private BigDecimal principalAmount;
    private BigDecimal outstandingAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private BigDecimal emiAmount;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextEmiDate;

    /** Loan status (e.g. ACTIVE, CLOSED, OVERDUE, PENDING_APPROVAL). */
    private String status;

    private String purpose;
}

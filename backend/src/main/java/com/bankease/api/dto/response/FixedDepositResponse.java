package com.bankease.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for a Fixed Deposit account.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedDepositResponse {

    private Long id;

    /** Unique FD reference number (e.g. FD20241025XXXXXX). */
    private String fdNumber;

    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;

    private LocalDate startDate;
    private LocalDate maturityDate;

    /** Projected maturity amount (principal + interest). */
    private BigDecimal maturityAmount;

    /** Interest payout type (e.g. MONTHLY, QUARTERLY, AT_MATURITY). */
    private String interestPayoutType;

    /** FD status (e.g. ACTIVE, MATURED, CLOSED, PREMATURE_CLOSED). */
    private String status;
}

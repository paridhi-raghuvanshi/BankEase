package com.bankease.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO for opening a Recurring Deposit account.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringDepositRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Monthly installment amount is required")
    @DecimalMin(value = "100.00", message = "Minimum monthly installment is ₹100")
    private BigDecimal monthlyInstallment;

    @NotNull(message = "Tenure in months is required")
    @Min(value = 6, message = "Minimum RD tenure is 6 months")
    @Max(value = 120, message = "Maximum RD tenure is 120 months (10 years)")
    private Integer tenureMonths;

    /** Annual interest rate; typically populated by the system during creation. */
    private BigDecimal interestRate;
}

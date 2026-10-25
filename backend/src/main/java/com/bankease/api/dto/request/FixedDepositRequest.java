package com.bankease.api.dto.request;

import com.bankease.api.model.enums.InterestPayoutType;
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
 * Request DTO for creating a Fixed Deposit.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedDepositRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum FD amount is ₹1,000")
    private BigDecimal principalAmount;

    @NotNull(message = "Tenure in months is required")
    @Min(value = 1, message = "Minimum FD tenure is 1 month")
    @Max(value = 120, message = "Maximum FD tenure is 120 months (10 years)")
    private Integer tenureMonths;

    @NotNull(message = "Interest payout type is required")
    private InterestPayoutType interestPayoutType;

    /** Interest rate as an annual percentage; set by the system but may be supplied for preview. */
    private BigDecimal interestRate;
}

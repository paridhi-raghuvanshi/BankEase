package com.bankease.api.dto.request;

import com.bankease.api.model.enums.LoanType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO for submitting a loan application.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Loan type is required")
    private LoanType loanType;

    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "10000.00", message = "Minimum loan amount is ₹10,000")
    private BigDecimal principalAmount;

    @NotNull(message = "Loan tenure is required")
    @Min(value = 6, message = "Minimum loan tenure is 6 months")
    @Max(value = 360, message = "Maximum loan tenure is 360 months (30 years)")
    private Integer tenureMonths;

    @Size(max = 500, message = "Purpose description must not exceed 500 characters")
    private String purpose;
}

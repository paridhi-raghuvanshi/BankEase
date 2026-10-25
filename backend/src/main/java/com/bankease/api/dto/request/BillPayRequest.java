package com.bankease.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request DTO for paying a bill through the BankEase platform.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillPayRequest {

    @NotNull(message = "Biller ID is required")
    private Long billerId;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "1.00", message = "Payment amount must be at least ₹1.00")
    private BigDecimal amount;

    @NotBlank(message = "Bill period is required")
    private String billPeriod;
}

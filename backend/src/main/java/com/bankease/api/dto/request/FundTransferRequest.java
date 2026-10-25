package com.bankease.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
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
 * Request DTO for initiating a fund transfer between accounts.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundTransferRequest {

    /** Supported transfer modes within BankEase. */
    public enum TransferMode {
        NEFT, RTGS, IMPS
    }

    @NotBlank(message = "Source account number is required")
    private String sourceAccountNumber;

    @NotBlank(message = "Destination account number is required")
    private String destinationAccountNumber;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "1.00", message = "Transfer amount must be at least ₹1.00")
    private BigDecimal amount;

    @NotNull(message = "Transfer mode is required")
    private TransferMode mode;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /** Optional reference to a saved beneficiary; null for ad-hoc transfers. */
    private Long beneficiaryId;
}

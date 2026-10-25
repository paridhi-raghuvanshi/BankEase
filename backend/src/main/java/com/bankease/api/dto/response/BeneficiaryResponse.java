package com.bankease.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response DTO for a saved payment beneficiary.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryResponse {

    private Long id;
    private String nickname;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String branchName;

    /** Beneficiary status (e.g. ACTIVE, INACTIVE). */
    private String status;

    /** Whether this beneficiary account has been verified by the system. */
    private boolean verified;

    private LocalDateTime createdAt;
}

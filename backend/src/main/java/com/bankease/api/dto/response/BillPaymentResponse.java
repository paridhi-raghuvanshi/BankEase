package com.bankease.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO returned after a successful or failed bill payment.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillPaymentResponse {

    private Long id;
    private String billerName;
    private BigDecimal amount;

    /** The billing period covered by this payment (e.g. "OCT-2024"). */
    private String billPeriod;

    /** Unique confirmation number issued by the biller or payment gateway. */
    private String confirmationNumber;

    /** Payment status (e.g. SUCCESS, FAILED, PENDING). */
    private String status;

    private LocalDateTime paidAt;
}

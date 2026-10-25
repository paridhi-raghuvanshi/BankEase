package com.bankease.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for a registered biller associated with a user account.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillerResponse {

    private Long id;
    private String billerName;

    /** Biller category (e.g. ELECTRICITY, WATER, GAS, BROADBAND). */
    private String category;

    private String consumerId;
    private String consumerName;
    private String nickname;

    /** Latest outstanding bill amount fetched from the biller. */
    private BigDecimal dueAmount;

    /** Due date of the current billing cycle. */
    private LocalDate dueDate;

    /** Biller registration status (e.g. ACTIVE, INACTIVE). */
    private String status;
}

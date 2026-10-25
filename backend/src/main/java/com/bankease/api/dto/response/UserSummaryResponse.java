package com.bankease.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Lightweight user summary embedded in auth and profile responses.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;
    private String customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    /** Role names (e.g. "ROLE_USER", "ROLE_ADMIN"). */
    private Set<String> roles;

    /** Account status (e.g. ACTIVE, SUSPENDED, CLOSED). */
    private String status;
}

package com.bankease.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO returned after a successful login or token refresh.
 * Contains both the short-lived access token and the long-lived refresh token.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Seconds until the access token expires. */
    private long expiresIn;

    /** Lightweight summary of the authenticated user. */
    private UserSummaryResponse user;
}

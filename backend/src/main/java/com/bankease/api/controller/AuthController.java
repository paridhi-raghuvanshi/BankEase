package com.bankease.api.controller;

import com.bankease.api.dto.request.ChangePasswordRequest;
import com.bankease.api.dto.request.LoginRequest;
import com.bankease.api.dto.request.RefreshTokenRequest;
import com.bankease.api.dto.request.RegisterRequest;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.AuthResponse;
import com.bankease.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication-related operations in BankEase.
 *
 * <p>Handles user registration, login, token refresh, logout, and password changes.
 * All responses are wrapped in {@link ApiResponse} for a consistent API envelope.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, token management, and password operations.")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new BankEase user account.
     *
     * @param request the registration payload containing personal and credential details
     * @return {@link AuthResponse} with JWT access and refresh tokens
     */
    @Operation(summary = "Register a new user", description = "Creates a new user account and returns JWT tokens upon successful registration.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Registration request received for email: {}", request.getEmail());
        AuthResponse authResponse = authService.register(request);
        log.info("User registered successfully: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully.", authResponse));
    }

    /**
     * Authenticates a user and returns JWT tokens.
     *
     * <p>The caller's IP address is captured from {@link HttpServletRequest} for
     * audit logging and fraud detection purposes.</p>
     *
     * @param request    the login credentials
     * @param httpRequest the incoming HTTP request (used to extract client IP)
     * @return {@link AuthResponse} containing access and refresh tokens
     */
    @Operation(summary = "Login", description = "Authenticates a user with email and password, returning JWT access and refresh tokens.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = resolveClientIp(httpRequest);
        log.info("Login attempt for email: {} from IP: {}", request.getEmail(), clientIp);
        AuthResponse authResponse = authService.login(request, clientIp);
        log.info("Login successful for email: {}", request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Login successful.", authResponse));
    }

    /**
     * Generates a new access token using a valid refresh token.
     *
     * @param request the payload containing the refresh token string
     * @return a new {@link AuthResponse} with a refreshed access token
     */
    @Operation(summary = "Refresh access token", description = "Exchanges a valid refresh token for a new JWT access token.")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.debug("Token refresh requested.");
        AuthResponse authResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully.", authResponse));
    }

    /**
     * Logs out the currently authenticated user by invalidating their refresh token.
     *
     * @param userDetails the authenticated user's details injected by Spring Security
     * @return a success acknowledgement message
     */
    @Operation(summary = "Logout", description = "Invalidates the current user's refresh token and clears the session.")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        log.info("Logout request for user: {}", email);
        authService.logout(email);
        log.info("User logged out successfully: {}", email);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully.", null));
    }

    /**
     * Changes the password for the currently authenticated user.
     *
     * @param request     the payload containing the current and new passwords
     * @param userDetails the authenticated principal
     * @return a success acknowledgement message
     */
    @Operation(summary = "Change password", description = "Allows an authenticated user to change their account password.")
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails.getUsername();
        log.info("Password change request for user: {}", email);
        authService.changePassword(email, request);
        log.info("Password changed successfully for user: {}", email);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully.", null));
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Resolves the real client IP address, accounting for reverse-proxy headers.
     *
     * @param request the incoming HTTP request
     * @return the best-guess client IP address string
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // X-Forwarded-For may contain a comma-separated list; the first entry is the client IP.
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

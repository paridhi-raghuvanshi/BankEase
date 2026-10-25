package com.bankease.api.service;

import com.bankease.api.dto.request.AuthRequest.ChangePasswordRequest;
import com.bankease.api.dto.request.AuthRequest.LoginRequest;
import com.bankease.api.dto.request.AuthRequest.RegisterRequest;
import com.bankease.api.dto.response.AuthResponse;
import com.bankease.api.entity.AuditLog;
import com.bankease.api.entity.RefreshToken;
import com.bankease.api.entity.Role;
import com.bankease.api.entity.User;
import com.bankease.api.exception.AccountLockedException;
import com.bankease.api.exception.BadCredentialsException;
import com.bankease.api.exception.InvalidTokenException;
import com.bankease.api.exception.ResourceNotFoundException;
import com.bankease.api.repository.RefreshTokenRepository;
import com.bankease.api.repository.RoleRepository;
import com.bankease.api.repository.UserRepository;
import com.bankease.api.security.JwtTokenProvider;
import com.bankease.api.util.CustomerIdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Core authentication service for BankEase.
 * <p>
 * Handles user registration, login (with brute-force protection), JWT/refresh-token
 * lifecycle, password changes, and logout. Every sensitive operation is recorded
 * via {@link AuditLogService}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Maximum consecutive failed login attempts before account lockout. */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /** Duration (in minutes) for which an account is locked after too many failures. */
    private static final int LOCK_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final CustomerIdGenerator customerIdGenerator;

    // -------------------------------------------------------------------------
    // Registration
    // -------------------------------------------------------------------------

    /**
     * Registers a new customer account.
     * <ol>
     *   <li>Generates a unique customer ID.</li>
     *   <li>Encodes the raw password.</li>
     *   <li>Assigns the {@code ROLE_CUSTOMER} role.</li>
     *   <li>Persists the user.</li>
     *   <li>Issues JWT and refresh tokens.</li>
     * </ol>
     *
     * @param request the registration payload
     * @return an {@link AuthResponse} containing access and refresh tokens
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email={}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email address is already registered: " + request.getEmail());
        }

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_CUSTOMER not found"));

        String customerId = customerIdGenerator.generate();

        User user = User.builder()
                .customerId(customerId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(customerRole))
                .enabled(true)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
        log.info("User registered: id={}, customerId={}", saved.getId(), saved.getCustomerId());

        String accessToken = jwtTokenProvider.generateAccessToken(saved);
        RefreshToken refreshToken = createAndSaveRefreshToken(saved);

        return buildAuthResponse(saved, accessToken, refreshToken.getToken());
    }

    // -------------------------------------------------------------------------
    // Login
    // -------------------------------------------------------------------------

    /**
     * Authenticates a user with email and password.
     * <p>
     * Implements brute-force protection: after {@value MAX_FAILED_ATTEMPTS} consecutive
     * failures the account is locked for {@value LOCK_DURATION_MINUTES} minutes.
     * A successful login resets the failure counter and generates fresh tokens.
     * </p>
     *
     * @param request the login credentials
     * @param httpRequest the originating HTTP request (used for audit metadata)
     * @return an {@link AuthResponse} on successful authentication
     * @throws AccountLockedException   if the account is currently locked
     * @throws BadCredentialsException  if the password does not match
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Login attempt for email={} from ip={}", request.getEmail(), ip);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed – unknown email={}", request.getEmail());
                    return new BadCredentialsException("Invalid email or password");
                });

        // Check lockout
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            log.warn("Login attempt on locked account userId={}", user.getId());
            auditLogService.log(user.getId(), user.getEmail(), "LOGIN_BLOCKED",
                    "Account locked until " + user.getLockedUntil(), null, null,
                    ip, userAgent, AuditLog.AuditResult.FAILURE);
            throw new AccountLockedException(
                    "Account is locked until " + user.getLockedUntil()
                            + ". Please try again later or contact support.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int failures = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failures);

            if (failures >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                log.warn("Account locked for userId={} after {} failed attempts", user.getId(), failures);
                auditLogService.log(user.getId(), user.getEmail(), "ACCOUNT_LOCKED",
                        "Account locked after " + failures + " failed attempts", null, null,
                        ip, userAgent, AuditLog.AuditResult.FAILURE);
            } else {
                auditLogService.log(user.getId(), user.getEmail(), "LOGIN_FAILED",
                        "Invalid password, attempt " + failures, null, null,
                        ip, userAgent, AuditLog.AuditResult.FAILURE);
            }

            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password");
        }

        // Successful login – reset counters
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        RefreshToken refreshToken = createAndSaveRefreshToken(user);

        auditLogService.log(user.getId(), user.getEmail(), "LOGIN",
                "Successful login", null, null, ip, userAgent, AuditLog.AuditResult.SUCCESS);

        log.info("Login successful for userId={}", user.getId());
        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    // -------------------------------------------------------------------------
    // Token Refresh
    // -------------------------------------------------------------------------

    /**
     * Issues a new access token in exchange for a valid, unexpired refresh token.
     *
     * @param token       the raw refresh token string supplied by the client
     * @param httpRequest the originating HTTP request (used for audit metadata)
     * @return a new {@link AuthResponse} with a fresh access token and the same refresh token
     * @throws InvalidTokenException if the token is not found, expired, or revoked
     */
    @Transactional
    public AuthResponse refreshToken(String token, HttpServletRequest httpRequest) {
        log.debug("Processing refresh token request");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        String ip = extractClientIp(httpRequest);
        auditLogService.log(user.getId(), user.getEmail(), "TOKEN_REFRESH",
                "Access token refreshed", ip);

        return buildAuthResponse(user, newAccessToken, refreshToken.getToken());
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    /**
     * Revokes the given refresh token, effectively logging the user out of that session.
     *
     * @param token the raw refresh token to revoke
     */
    @Transactional
    public void logout(String token) {
        log.info("Processing logout for refresh token");
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(rt);
            log.info("Refresh token revoked for userId={}", rt.getUser().getId());
        });
    }

    // -------------------------------------------------------------------------
    // Change Password
    // -------------------------------------------------------------------------

    /**
     * Changes a user's password after verifying the current password.
     * <p>
     * All existing refresh tokens for the user are revoked so that sessions on
     * other devices are invalidated upon a password change.
     * </p>
     *
     * @param userId  the ID of the user requesting the change
     * @param request the current and new password payload
     * @throws ResourceNotFoundException if the user does not exist
     * @throws BadCredentialsException   if the current password is incorrect
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("Password change requested for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all active refresh tokens so other sessions are invalidated
        refreshTokenRepository.findAllByUserAndRevoked(user, false).forEach(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(LocalDateTime.now());
        });
        refreshTokenRepository.saveAll(
                refreshTokenRepository.findAllByUserAndRevoked(user, false));

        auditLogService.log(userId, user.getEmail(), "PASSWORD_CHANGED",
                "User changed their password", null);

        log.info("Password changed successfully for userId={}", userId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Creates, persists, and returns a new refresh token for the given user.
     *
     * @param user the user for whom the token is created
     * @return the persisted {@link RefreshToken} entity
     */
    private RefreshToken createAndSaveRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(jwtTokenProvider.generateRefreshTokenValue())
                .expiresAt(LocalDateTime.now().plusDays(
                        jwtTokenProvider.getRefreshTokenValidityDays()))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Assembles an {@link AuthResponse} from its constituent parts.
     *
     * @param user         the authenticated user
     * @param accessToken  the signed JWT access token
     * @param refreshToken the raw refresh token string
     * @return the populated response DTO
     */
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .customerId(user.getCustomerId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .toList())
                .expiresIn(jwtTokenProvider.getAccessTokenValiditySeconds())
                .build();
    }

    /**
     * Extracts the true client IP from the request, honouring the {@code X-Forwarded-For} proxy header.
     *
     * @param request the HTTP request
     * @return the best-guess client IP address string
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

package com.bankease.api.service;

import com.bankease.api.dto.request.ChangePasswordRequest;
import com.bankease.api.dto.request.LoginRequest;
import com.bankease.api.dto.request.RegisterRequest;
import com.bankease.api.dto.response.AuthResponse;
import com.bankease.api.dto.response.UserSummaryResponse;
import com.bankease.api.entity.RefreshToken;
import com.bankease.api.entity.Role;
import com.bankease.api.entity.User;
import com.bankease.api.exception.AccountLockedException;
import com.bankease.api.exception.BadCredentialsException;
import com.bankease.api.exception.BankEaseException;
import com.bankease.api.exception.InvalidTokenException;
import com.bankease.api.exception.ResourceNotFoundException;
import com.bankease.api.repository.RefreshTokenRepository;
import com.bankease.api.repository.RoleRepository;
import com.bankease.api.repository.UserRepository;
import com.bankease.api.security.JwtTokenProvider;
import com.bankease.api.util.CustomerIdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final CustomerIdGenerator customerIdGenerator;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BankEaseException("Email address is already in use", HttpStatus.CONFLICT);
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BankEaseException("Phone number is already registered", HttpStatus.CONFLICT);
        }

        Role customerRole = roleRepository.findByName(Role.RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(Role.RoleName.ROLE_CUSTOMER)
                                .description("Standard banking customer")
                                .build()
                ));

        String customerId = customerIdGenerator.generate();

        User user = User.builder()
                .customerId(customerId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase().trim())
                .phone(request.getPhone().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .status(User.UserStatus.ACTIVE)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .build();

        user = userRepository.save(user);

        org.springframework.security.core.userdetails.UserDetails userDetails = toUserDetails(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        RefreshToken refreshToken = createAndSaveRefreshToken(user);

        auditLogService.log(user.getId(), user.getEmail(), "REGISTER", "New user registered: " + user.getCustomerId(), null);

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email address or password"));

        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new AccountLockedException("Account is locked. Try again after " + user.getLockedUntil());
            } else {
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
                user.setStatus(User.UserStatus.LOCKED);
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid email address or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        org.springframework.security.core.userdetails.UserDetails userDetails = toUserDetails(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        RefreshToken refreshToken = createAndSaveRefreshToken(user);

        auditLogService.log(user.getId(), user.getEmail(), "LOGIN", "Login success from IP " + ipAddress, ipAddress);

        return buildAuthResponse(user, accessToken, refreshToken.getToken());
    }

    @Transactional
    public AuthResponse refreshToken(String token, HttpServletRequest httpRequest) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (!refreshToken.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        org.springframework.security.core.userdetails.UserDetails userDetails = toUserDetails(user);
        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);

        return buildAuthResponse(user, newAccessToken, refreshToken.getToken());
    }

    @Transactional
    public void logout(String token) {
        if (token != null) {
            refreshTokenRepository.findByToken(token).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password does not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllUserTokens(userId);
    }

    private RefreshToken createAndSaveRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(jwtTokenProvider.generateRefreshTokenValue())
                .expiresAt(LocalDateTime.now().plusDays(jwtTokenProvider.getRefreshTokenValidityDays()))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        UserSummaryResponse userSummary = UserSummaryResponse.builder()
                .id(user.getId())
                .customerId(user.getCustomerId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .roles(user.getRoles().stream()
                        .map(r -> r.getName().name())
                        .collect(Collectors.toSet()))
                .build();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValiditySeconds())
                .user(userSummary)
                .build();
    }

    private org.springframework.security.core.userdetails.UserDetails toUserDetails(User user) {
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip : "127.0.0.1";
    }
}

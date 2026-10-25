package com.bankease.api.controller;

import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.AuditLogResponse;
import com.bankease.api.dto.response.UserSummaryResponse;
import com.bankease.api.model.User;
import com.bankease.api.repository.UserRepository;
import com.bankease.api.service.AuditLogService;
import com.bankease.api.service.UserService;
import com.bankease.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile and audit log operations in BankEase.
 *
 * <p>Exposes endpoints for the currently authenticated user to retrieve their
 * profile summary and a paginated audit trail of account activities.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for user profile retrieval and audit log access.")
public class UserController {

    private final UserService userService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Returns the profile summary of the currently authenticated user.
     *
     * <p>The response includes non-sensitive personal details such as name, email,
     * phone number, KYC status, and account creation date.</p>
     *
     * @return a {@link UserSummaryResponse} for the authenticated user
     */
    @Operation(summary = "Get current user profile", description = "Returns the profile summary (name, email, KYC status, etc.) of the currently authenticated user.")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getCurrentUserProfile() {
        User currentUser = securityUtils.getCurrentUser(userRepository);
        log.debug("Fetching profile for userId={}", currentUser.getId());
        UserSummaryResponse summary = userService.getUserSummary(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved successfully.", summary));
    }

    /**
     * Returns a paginated audit log of activity events for the currently authenticated user.
     *
     * <p>Events include login attempts, transfers, password changes, and other sensitive
     * account actions recorded by the system for compliance and security review.</p>
     *
     * @param page the zero-based page index (default: 0)
     * @param size the number of records per page (default: 20)
     * @return a {@link Page} of {@link AuditLogResponse} records
     */
    @Operation(summary = "Get audit log", description = "Returns a paginated audit trail of account activity events for the currently authenticated user.")
    @GetMapping("/audit-log")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLog(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching audit log for userId={} | page={} size={}", userId, page, size);
        Page<AuditLogResponse> auditLog = auditLogService.getAuditLogByUserId(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Audit log retrieved successfully.", auditLog));
    }
}

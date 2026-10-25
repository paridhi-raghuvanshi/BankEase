package com.bankease.api.controller;

import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.AuditLogResponse;
import com.bankease.api.dto.response.PagedResponse;
import com.bankease.api.dto.response.UserSummaryResponse;
import com.bankease.api.service.UserService;
import com.bankease.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User profile and audit trail APIs")
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        UserSummaryResponse response = userService.getCurrentUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/audit-log")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get compliance audit logs for current user")
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = securityUtils.getCurrentUserId();
        PagedResponse<AuditLogResponse> response = userService.getUserAuditLogs(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

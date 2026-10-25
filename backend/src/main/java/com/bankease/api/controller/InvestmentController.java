package com.bankease.api.controller;

import com.bankease.api.dto.request.FixedDepositRequest;
import com.bankease.api.dto.request.RecurringDepositRequest;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.FixedDepositResponse;
import com.bankease.api.dto.response.RecurringDepositResponse;
import com.bankease.api.service.InvestmentService;
import com.bankease.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Investments", description = "Term deposits (Fixed & Recurring Deposits) management APIs")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final SecurityUtils securityUtils;

    @GetMapping("/fd")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List customer Fixed Deposits")
    public ResponseEntity<ApiResponse<List<FixedDepositResponse>>> getFixedDeposits() {
        Long userId = securityUtils.getCurrentUserId();
        List<FixedDepositResponse> list = investmentService.getFixedDeposits(userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/fd/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Fixed Deposit details by ID")
    public ResponseEntity<ApiResponse<FixedDepositResponse>> getFixedDepositById(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        FixedDepositResponse fd = investmentService.getFixedDepositById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(fd));
    }

    @PostMapping("/fd")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Open a Fixed Deposit")
    public ResponseEntity<ApiResponse<FixedDepositResponse>> createFixedDeposit(@Valid @RequestBody FixedDepositRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        FixedDepositResponse response = investmentService.createFixedDeposit(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Fixed Deposit created successfully", response));
    }

    @GetMapping("/rd")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List customer Recurring Deposits")
    public ResponseEntity<ApiResponse<List<RecurringDepositResponse>>> getRecurringDeposits() {
        Long userId = securityUtils.getCurrentUserId();
        List<RecurringDepositResponse> list = investmentService.getRecurringDeposits(userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/rd/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get Recurring Deposit details by ID")
    public ResponseEntity<ApiResponse<RecurringDepositResponse>> getRecurringDepositById(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        RecurringDepositResponse rd = investmentService.getRecurringDepositById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(rd));
    }

    @PostMapping("/rd")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Open a Recurring Deposit")
    public ResponseEntity<ApiResponse<RecurringDepositResponse>> createRecurringDeposit(@Valid @RequestBody RecurringDepositRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        RecurringDepositResponse response = investmentService.createRecurringDeposit(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Recurring Deposit created successfully", response));
    }
}

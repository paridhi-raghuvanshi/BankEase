package com.bankease.api.controller;

import com.bankease.api.dto.request.FixedDepositRequest;
import com.bankease.api.dto.request.RecurringDepositRequest;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.FixedDepositResponse;
import com.bankease.api.dto.response.RecurringDepositResponse;
import com.bankease.api.repository.UserRepository;
import com.bankease.api.service.InvestmentService;
import com.bankease.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for investment product operations in BankEase.
 *
 * <p>Supports Fixed Deposit (FD) and Recurring Deposit (RD) lifecycle management,
 * including creation and retrieval by ID or for the current user.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Investments", description = "Endpoints for managing Fixed Deposits (FD) and Recurring Deposits (RD).")
public class InvestmentController {

    private final InvestmentService investmentService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    // -------------------------------------------------------------------------
    // Fixed Deposits
    // -------------------------------------------------------------------------

    /**
     * Returns all Fixed Deposits belonging to the currently authenticated user.
     *
     * @return a list of {@link FixedDepositResponse} objects
     */
    @Operation(summary = "Get Fixed Deposits", description = "Retrieves all Fixed Deposit accounts associated with the currently authenticated user.")
    @GetMapping("/fd")
    public ResponseEntity<ApiResponse<List<FixedDepositResponse>>> getFixedDeposits() {
        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching Fixed Deposits for userId={}", userId);
        List<FixedDepositResponse> fds = investmentService.getFixedDepositsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Fixed Deposits retrieved successfully.", fds));
    }

    /**
     * Retrieves a specific Fixed Deposit by its database ID.
     *
     * <p>The service validates that the FD belongs to the currently authenticated user.</p>
     *
     * @param id the database ID of the Fixed Deposit
     * @return the matching {@link FixedDepositResponse}
     */
    @Operation(summary = "Get Fixed Deposit by ID", description = "Fetches a specific Fixed Deposit by its ID. The FD must belong to the current user.")
    @GetMapping("/fd/{id}")
    public ResponseEntity<ApiResponse<FixedDepositResponse>> getFixedDepositById(
            @Parameter(description = "The ID of the Fixed Deposit", required = true)
            @PathVariable Long id) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching FD id={} for userId={}", id, userId);
        FixedDepositResponse fd = investmentService.getFixedDepositById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Fixed Deposit retrieved successfully.", fd));
    }

    /**
     * Creates a new Fixed Deposit for the currently authenticated user.
     *
     * @param request the FD creation payload (principal, tenure, interest rate, linked account)
     * @return the newly created {@link FixedDepositResponse}
     */
    @Operation(summary = "Create Fixed Deposit", description = "Opens a new Fixed Deposit account, debiting the principal from the user's linked account.")
    @PostMapping("/fd")
    public ResponseEntity<ApiResponse<FixedDepositResponse>> createFixedDeposit(
            @Valid @RequestBody FixedDepositRequest request) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.info("Creating Fixed Deposit for userId={} | principal={} tenure={}",
                userId, request.getPrincipal(), request.getTenureMonths());
        FixedDepositResponse fd = investmentService.createFixedDeposit(request, userId);
        log.info("Fixed Deposit created | id={} userId={}", fd.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fixed Deposit created successfully.", fd));
    }

    // -------------------------------------------------------------------------
    // Recurring Deposits
    // -------------------------------------------------------------------------

    /**
     * Returns all Recurring Deposits belonging to the currently authenticated user.
     *
     * @return a list of {@link RecurringDepositResponse} objects
     */
    @Operation(summary = "Get Recurring Deposits", description = "Retrieves all Recurring Deposit accounts associated with the currently authenticated user.")
    @GetMapping("/rd")
    public ResponseEntity<ApiResponse<List<RecurringDepositResponse>>> getRecurringDeposits() {
        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching Recurring Deposits for userId={}", userId);
        List<RecurringDepositResponse> rds = investmentService.getRecurringDepositsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Recurring Deposits retrieved successfully.", rds));
    }

    /**
     * Retrieves a specific Recurring Deposit by its database ID.
     *
     * <p>The service validates that the RD belongs to the currently authenticated user.</p>
     *
     * @param id the database ID of the Recurring Deposit
     * @return the matching {@link RecurringDepositResponse}
     */
    @Operation(summary = "Get Recurring Deposit by ID", description = "Fetches a specific Recurring Deposit by its ID. The RD must belong to the current user.")
    @GetMapping("/rd/{id}")
    public ResponseEntity<ApiResponse<RecurringDepositResponse>> getRecurringDepositById(
            @Parameter(description = "The ID of the Recurring Deposit", required = true)
            @PathVariable Long id) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching RD id={} for userId={}", id, userId);
        RecurringDepositResponse rd = investmentService.getRecurringDepositById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Recurring Deposit retrieved successfully.", rd));
    }

    /**
     * Creates a new Recurring Deposit for the currently authenticated user.
     *
     * @param request the RD creation payload (monthly instalment, tenure, linked account)
     * @return the newly created {@link RecurringDepositResponse}
     */
    @Operation(summary = "Create Recurring Deposit", description = "Opens a new Recurring Deposit account with a defined monthly instalment amount and tenure.")
    @PostMapping("/rd")
    public ResponseEntity<ApiResponse<RecurringDepositResponse>> createRecurringDeposit(
            @Valid @RequestBody RecurringDepositRequest request) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.info("Creating Recurring Deposit for userId={} | monthlyInstalment={} tenure={}",
                userId, request.getMonthlyInstalment(), request.getTenureMonths());
        RecurringDepositResponse rd = investmentService.createRecurringDeposit(request, userId);
        log.info("Recurring Deposit created | id={} userId={}", rd.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recurring Deposit created successfully.", rd));
    }
}

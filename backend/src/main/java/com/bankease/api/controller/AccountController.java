package com.bankease.api.controller;

import com.bankease.api.dto.response.AccountResponse;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.BalanceResponse;
import com.bankease.api.dto.response.TransactionResponse;
import com.bankease.api.repository.UserRepository;
import com.bankease.api.service.AccountService;
import com.bankease.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for bank account operations in BankEase.
 *
 * <p>Provides endpoints to retrieve account information, check balances, and
 * fetch transaction statements (paginated or by date range) for the currently
 * authenticated user.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Accounts", description = "Endpoints for managing and querying bank accounts.")
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Returns all accounts belonging to the currently authenticated user.
     *
     * @return a list of {@link AccountResponse} objects
     */
    @Operation(summary = "Get all accounts", description = "Retrieves all bank accounts associated with the currently authenticated user.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccountsByUserId() {
        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching all accounts for userId: {}", userId);
        List<AccountResponse> accounts = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully.", accounts));
    }

    /**
     * Returns account details for a specific account number, validating ownership by the current user.
     *
     * @param accountNumber the unique account number to look up
     * @return an {@link AccountResponse} for the requested account
     */
    @Operation(summary = "Get account by number", description = "Retrieves a specific bank account by its account number. Only accessible by the account owner.")
    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByNumber(
            @Parameter(description = "The unique account number", required = true)
            @PathVariable String accountNumber) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching account {} for userId: {}", accountNumber, userId);
        AccountResponse account = accountService.getAccountByNumber(accountNumber, userId);
        return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully.", account));
    }

    /**
     * Returns the current balance for a specific account.
     *
     * @param accountNumber the account number whose balance is being queried
     * @return a {@link BalanceResponse} with available and current balance
     */
    @Operation(summary = "Get account balance", description = "Fetches the current and available balance for a given account number.")
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @Parameter(description = "The account number to check balance for", required = true)
            @PathVariable String accountNumber) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching balance for account {} (userId: {})", accountNumber, userId);
        BalanceResponse balance = accountService.getBalance(accountNumber, userId);
        return ResponseEntity.ok(ApiResponse.success("Balance retrieved successfully.", balance));
    }

    /**
     * Returns a paginated transaction history (statement) for the specified account.
     *
     * @param accountNumber the account number to retrieve the statement for
     * @param page          the zero-based page index (default: 0)
     * @param size          the number of records per page (default: 20)
     * @return a {@link Page} of {@link TransactionResponse} records
     */
    @Operation(summary = "Get account statement", description = "Returns a paginated list of transactions for the specified account.")
    @GetMapping("/{accountNumber}/statement")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactionHistory(
            @Parameter(description = "The account number to retrieve statement for", required = true)
            @PathVariable String accountNumber,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching statement for account {} | page={} size={}", accountNumber, page, size);
        Page<TransactionResponse> statement = accountService.getTransactionHistory(accountNumber, userId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Statement retrieved successfully.", statement));
    }

    /**
     * Returns transactions for the specified account within a given date range (paginated).
     *
     * @param accountNumber the account number to query
     * @param fromDate      the inclusive start date (format: yyyy-MM-dd)
     * @param toDate        the inclusive end date (format: yyyy-MM-dd)
     * @param page          the zero-based page index (default: 0)
     * @param size          the number of records per page (default: 20)
     * @return a {@link Page} of {@link TransactionResponse} records within the date range
     */
    @Operation(summary = "Get statement by date range", description = "Returns a filtered, paginated list of transactions within a specified date range.")
    @GetMapping("/{accountNumber}/statement/range")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getByDateRange(
            @Parameter(description = "The account number to retrieve transactions for", required = true)
            @PathVariable String accountNumber,
            @Parameter(description = "Start date in yyyy-MM-dd format", required = true, example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "End date in yyyy-MM-dd format", required = true, example = "2025-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching statement for account {} | range={} to {} | page={} size={}",
                accountNumber, fromDate, toDate, page, size);
        Page<TransactionResponse> statement =
                accountService.getByDateRange(accountNumber, userId, fromDate, toDate, page, size);
        return ResponseEntity.ok(ApiResponse.success("Statement retrieved successfully.", statement));
    }
}

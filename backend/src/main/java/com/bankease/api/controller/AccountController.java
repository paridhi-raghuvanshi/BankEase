package com.bankease.api.controller;

import com.bankease.api.dto.response.AccountResponse;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.BalanceResponse;
import com.bankease.api.dto.response.PagedResponse;
import com.bankease.api.dto.response.TransactionResponse;
import com.bankease.api.service.AccountService;
import com.bankease.api.service.TransactionService;
import com.bankease.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Accounts", description = "Account management and statement APIs")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all accounts for current user")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAccounts() {
        Long userId = securityUtils.getCurrentUserId();
        List<AccountResponse> accounts = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/{accountNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get account details by account number")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountByNumber(@PathVariable String accountNumber) {
        Long userId = securityUtils.getCurrentUserId();
        AccountResponse account = accountService.getAccountByNumber(accountNumber, userId);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @GetMapping("/{accountNumber}/balance")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get real-time balance for an account")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@PathVariable String accountNumber) {
        Long userId = securityUtils.getCurrentUserId();
        BigDecimal balance = accountService.getBalance(accountNumber, userId);
        AccountResponse acc = accountService.getAccountByNumber(accountNumber, userId);
        BalanceResponse res = BalanceResponse.builder()
                .accountNumber(accountNumber)
                .balance(balance)
                .availableBalance(acc.getAvailableBalance())
                .currency(acc.getCurrency())
                .build();
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    @GetMapping("/{accountNumber}/statement")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get paginated account transaction statement")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getStatement(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = securityUtils.getCurrentUserId();
        AccountResponse acc = accountService.getAccountByNumber(accountNumber, userId);
        PagedResponse<TransactionResponse> statement = transactionService.getTransactionHistory(acc.getId(), userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(statement));
    }

    @GetMapping("/{accountNumber}/statement/range")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Filter account statement by date range")
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getStatementByRange(
            @PathVariable String accountNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = securityUtils.getCurrentUserId();
        AccountResponse acc = accountService.getAccountByNumber(accountNumber, userId);
        PagedResponse<TransactionResponse> statement = transactionService.getTransactionsByDateRange(
                acc.getId(), userId, fromDate, toDate, page, size);
        return ResponseEntity.ok(ApiResponse.success(statement));
    }
}

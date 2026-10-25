package com.bankease.api.controller;

import com.bankease.api.dto.request.FundTransferRequest;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.TransactionResponse;
import com.bankease.api.repository.UserRepository;
import com.bankease.api.service.TransactionService;
import com.bankease.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for fund transfer operations in BankEase.
 *
 * <p>Exposes endpoints to initiate fund transfers, query transfer history,
 * and retrieve individual transactions by reference number.
 * All operations are restricted to authenticated users.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Transfers", description = "Endpoints for initiating and querying fund transfers.")
public class TransferController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Initiates a fund transfer between accounts.
     *
     * <p>The source account must belong to the currently authenticated user. The
     * transfer is processed synchronously and a reference number is returned on success.</p>
     *
     * @param request the transfer payload containing source account, destination account, amount, and narration
     * @return the created {@link TransactionResponse} with reference number and status
     */
    @Operation(summary = "Transfer funds", description = "Initiates a fund transfer from the user's account to a specified destination account.")
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> transferFunds(
            @Valid @RequestBody FundTransferRequest request) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.info("Fund transfer request initiated by userId={} | from={} to={} amount={}",
                userId, request.getSourceAccountNumber(), request.getDestinationAccountNumber(), request.getAmount());
        TransactionResponse response = transactionService.transferFunds(request, userId);
        log.info("Fund transfer completed | referenceNumber={}", response.getReferenceNumber());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fund transfer initiated successfully.", response));
    }

    /**
     * Retrieves the paginated transaction history for the currently authenticated user.
     *
     * @param page the zero-based page index (default: 0)
     * @param size the number of records per page (default: 20)
     * @return a {@link Page} of {@link TransactionResponse} records
     */
    @Operation(summary = "Get transfer history", description = "Returns a paginated list of all fund transfer transactions for the currently authenticated user.")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactionHistory(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching transfer history for userId={} | page={} size={}", userId, page, size);
        Page<TransactionResponse> history = transactionService.getTransactionHistoryByUserId(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Transfer history retrieved successfully.", history));
    }

    /**
     * Retrieves a specific transaction by its unique reference number.
     *
     * @param referenceNumber the unique reference number of the transaction
     * @return the matching {@link TransactionResponse}
     */
    @Operation(summary = "Get transaction by reference", description = "Fetches a single transaction record using its unique reference number.")
    @GetMapping("/{referenceNumber}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionByRef(
            @Parameter(description = "The unique transaction reference number", required = true)
            @PathVariable String referenceNumber) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Looking up transaction ref={} for userId={}", referenceNumber, userId);
        TransactionResponse transaction = transactionService.getTransactionByReference(referenceNumber, userId);
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully.", transaction));
    }
}

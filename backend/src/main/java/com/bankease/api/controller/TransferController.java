package com.bankease.api.controller;

import com.bankease.api.dto.request.FundTransferRequest;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.PagedResponse;
import com.bankease.api.dto.response.TransactionResponse;
import com.bankease.api.service.TransactionService;
import com.bankease.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Fund Transfers", description = "Fund transfer operations (IMPS, NEFT, RTGS)")
public class TransferController {

    private final TransactionService transactionService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Transfer funds to an account")
    public ResponseEntity<ApiResponse<TransactionResponse>> transferFunds(@Valid @RequestBody FundTransferRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        TransactionResponse response = transactionService.transferFunds(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", response));
    }

    @GetMapping("/{referenceNumber}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction details by reference number")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionByRef(@PathVariable String referenceNumber) {
        Long userId = securityUtils.getCurrentUserId();
        TransactionResponse response = transactionService.getTransactionByRef(referenceNumber, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

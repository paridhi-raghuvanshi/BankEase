package com.bankease.api.controller;

import com.bankease.api.dto.request.LoanApplicationRequest;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.EmiCalculationResponse;
import com.bankease.api.dto.response.LoanResponse;
import com.bankease.api.service.LoanService;
import com.bankease.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Loans", description = "Loan applications and EMI calculation APIs")
public class LoanController {

    private final LoanService loanService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List loans for current customer")
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getLoans() {
        Long userId = securityUtils.getCurrentUserId();
        List<LoanResponse> loans = loanService.getLoans(userId);
        return ResponseEntity.ok(ApiResponse.success(loans));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get loan details by ID")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanById(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        LoanResponse loan = loanService.getLoanById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(loan));
    }

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit retail/commercial loan application")
    public ResponseEntity<ApiResponse<LoanResponse>> applyForLoan(@Valid @RequestBody LoanApplicationRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        LoanResponse response = loanService.applyForLoan(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Loan application submitted", response));
    }

    @GetMapping("/calculate-emi")
    @Operation(summary = "Calculate loan EMI, total interest, and payout schedule")
    public ResponseEntity<ApiResponse<EmiCalculationResponse>> calculateEmi(
            @RequestParam BigDecimal principal,
            @RequestParam BigDecimal rate,
            @RequestParam int tenureMonths) {
        Map<String, Object> map = loanService.calculateEmi(principal, rate, tenureMonths);
        EmiCalculationResponse response = EmiCalculationResponse.builder()
                .principalAmount((BigDecimal) map.get("principalAmount"))
                .interestRate((BigDecimal) map.get("interestRate"))
                .tenureMonths((Integer) map.get("tenureMonths"))
                .emiAmount((BigDecimal) map.get("emiAmount"))
                .totalPayment((BigDecimal) map.get("totalPayment"))
                .totalInterest((BigDecimal) map.get("totalInterest"))
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

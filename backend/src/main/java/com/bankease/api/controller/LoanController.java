package com.bankease.api.controller;

import com.bankease.api.dto.request.LoanApplicationRequest;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.EmiCalculationResponse;
import com.bankease.api.dto.response.LoanResponse;
import com.bankease.api.repository.UserRepository;
import com.bankease.api.service.LoanService;
import com.bankease.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for loan-related operations in BankEase.
 *
 * <p>Allows authenticated users to view their loans, apply for new loans,
 * and calculate EMI (Equated Monthly Instalment) for prospective loan parameters.</p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Loans", description = "Endpoints for loan management and EMI calculations.")
public class LoanController {

    private final LoanService loanService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Returns all loans associated with the currently authenticated user.
     *
     * @return a list of {@link LoanResponse} objects
     */
    @Operation(summary = "Get all loans", description = "Retrieves all active and historical loans for the currently authenticated user.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanResponse>>> getLoans() {
        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching loans for userId={}", userId);
        List<LoanResponse> loans = loanService.getLoansByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully.", loans));
    }

    /**
     * Retrieves a specific loan by its database ID.
     *
     * <p>The service validates that the loan belongs to the currently authenticated user.</p>
     *
     * @param id the database ID of the loan
     * @return the matching {@link LoanResponse}
     */
    @Operation(summary = "Get loan by ID", description = "Fetches detailed information for a specific loan. The loan must belong to the current user.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanResponse>> getLoanById(
            @Parameter(description = "The ID of the loan to retrieve", required = true)
            @PathVariable Long id) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching loan id={} for userId={}", id, userId);
        LoanResponse loan = loanService.getLoanById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Loan retrieved successfully.", loan));
    }

    /**
     * Submits a new loan application for the currently authenticated user.
     *
     * @param request the loan application payload (loan type, amount, tenure, purpose, etc.)
     * @return the created {@link LoanResponse} with application status
     */
    @Operation(summary = "Apply for loan", description = "Submits a new loan application. The application is queued for review based on the user's credit profile.")
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<LoanResponse>> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.info("Loan application submitted | userId={} type={} amount={}",
                userId, request.getLoanType(), request.getAmount());
        LoanResponse loan = loanService.applyForLoan(request, userId);
        log.info("Loan application created | loanId={} userId={}", loan.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Loan application submitted successfully.", loan));
    }

    /**
     * Calculates the EMI for a prospective loan based on principal, interest rate, and tenure.
     *
     * <p>Uses the standard reducing-balance formula:
     * <pre>
     *   EMI = P * r * (1 + r)^n / ((1 + r)^n - 1)
     * </pre>
     * where {@code P} = principal, {@code r} = monthly rate, {@code n} = tenure in months.</p>
     *
     * @param principal     the loan principal amount (must be greater than 0)
     * @param rate          the annual interest rate as a percentage (e.g., 8.5 for 8.5% p.a.)
     * @param tenureMonths  the loan tenure in months (must be at least 1)
     * @return an {@link EmiCalculationResponse} containing the monthly EMI and total payable amount
     */
    @Operation(summary = "Calculate EMI", description = "Calculates the Equated Monthly Instalment (EMI) for a prospective loan using the reducing-balance formula.")
    @GetMapping("/calculate-emi")
    public ResponseEntity<ApiResponse<EmiCalculationResponse>> calculateEmi(
            @Parameter(description = "Loan principal amount (must be > 0)", required = true, example = "500000.00")
            @RequestParam @DecimalMin(value = "0.01", message = "Principal must be greater than zero.") BigDecimal principal,

            @Parameter(description = "Annual interest rate as a percentage (e.g., 8.5 for 8.5% p.a.)", required = true, example = "8.5")
            @RequestParam @DecimalMin(value = "0.01", message = "Interest rate must be greater than zero.") BigDecimal rate,

            @Parameter(description = "Loan tenure in months (minimum 1)", required = true, example = "60")
            @RequestParam @Min(value = 1, message = "Tenure must be at least 1 month.") int tenureMonths) {

        log.debug("EMI calculation requested | principal={} rate={} tenureMonths={}", principal, rate, tenureMonths);
        EmiCalculationResponse emiResponse = loanService.calculateEmi(principal, rate, tenureMonths);
        return ResponseEntity.ok(ApiResponse.success("EMI calculated successfully.", emiResponse));
    }
}

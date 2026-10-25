package com.bankease.api.controller;

import com.bankease.api.dto.request.BillPayRequest;
import com.bankease.api.dto.request.BillerRequest;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.BillPaymentResponse;
import com.bankease.api.dto.response.BillerResponse;
import com.bankease.api.repository.UserRepository;
import com.bankease.api.service.BillPayService;
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

import java.util.List;

/**
 * REST controller for bill payment operations in BankEase.
 *
 * <p>Manages billers (utilities, subscriptions, etc.) and processes bill payments
 * on behalf of the currently authenticated user. Payment history is exposed
 * in a paginated format.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Bill Payments", description = "Endpoints for managing billers and processing bill payments.")
public class BillPayController {

    private final BillPayService billPayService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    // -------------------------------------------------------------------------
    // Biller Management
    // -------------------------------------------------------------------------

    /**
     * Returns all billers saved by the currently authenticated user.
     *
     * @return a list of {@link BillerResponse} objects
     */
    @Operation(summary = "Get billers", description = "Retrieves all saved billers for the currently authenticated user.")
    @GetMapping("/billers")
    public ResponseEntity<ApiResponse<List<BillerResponse>>> getBillers() {
        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching billers for userId={}", userId);
        List<BillerResponse> billers = billPayService.getBillersByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Billers retrieved successfully.", billers));
    }

    /**
     * Adds a new biller to the currently authenticated user's biller list.
     *
     * @param request the biller details (name, category, account/consumer reference)
     * @return the newly created {@link BillerResponse}
     */
    @Operation(summary = "Add biller", description = "Saves a new biller (e.g., electricity, internet) to the user's biller list.")
    @PostMapping("/billers")
    public ResponseEntity<ApiResponse<BillerResponse>> addBiller(
            @Valid @RequestBody BillerRequest request) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.info("Adding biller '{}' for userId={}", request.getBillerName(), userId);
        BillerResponse biller = billPayService.addBiller(request, userId);
        log.info("Biller added | id={} userId={}", biller.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Biller added successfully.", biller));
    }

    /**
     * Removes a saved biller by its ID.
     *
     * <p>The service validates that the biller is owned by the currently authenticated user.</p>
     *
     * @param id the database ID of the biller to delete
     * @return a success acknowledgement
     */
    @Operation(summary = "Delete biller", description = "Removes a biller from the user's saved list. The biller must be owned by the current user.")
    @DeleteMapping("/billers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBiller(
            @Parameter(description = "The ID of the biller to delete", required = true)
            @PathVariable Long id) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.info("Deleting biller id={} for userId={}", id, userId);
        billPayService.deleteBiller(id, userId);
        log.info("Biller id={} deleted for userId={}", id, userId);
        return ResponseEntity.ok(ApiResponse.success("Biller deleted successfully.", null));
    }

    // -------------------------------------------------------------------------
    // Bill Payments
    // -------------------------------------------------------------------------

    /**
     * Processes a bill payment for the currently authenticated user.
     *
     * @param request the bill payment payload (biller ID, amount, source account)
     * @return the resulting {@link BillPaymentResponse} with payment confirmation details
     */
    @Operation(summary = "Pay bill", description = "Processes a bill payment from the user's designated account to the specified biller.")
    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<BillPaymentResponse>> payBill(
            @Valid @RequestBody BillPayRequest request) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.info("Bill payment initiated | userId={} billerId={} amount={}",
                userId, request.getBillerId(), request.getAmount());
        BillPaymentResponse payment = billPayService.payBill(request, userId);
        log.info("Bill payment completed | referenceNumber={}", payment.getReferenceNumber());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bill payment processed successfully.", payment));
    }

    /**
     * Returns a paginated history of all bill payments made by the currently authenticated user.
     *
     * @param page the zero-based page index (default: 0)
     * @param size the number of records per page (default: 20)
     * @return a {@link Page} of {@link BillPaymentResponse} records
     */
    @Operation(summary = "Get payment history", description = "Returns a paginated list of bill payments made by the currently authenticated user.")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<BillPaymentResponse>>> getPaymentHistory(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of records per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching bill payment history for userId={} | page={} size={}", userId, page, size);
        Page<BillPaymentResponse> history = billPayService.getPaymentHistory(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved successfully.", history));
    }
}

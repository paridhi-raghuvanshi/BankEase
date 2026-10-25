package com.bankease.api.controller;

import com.bankease.api.dto.request.AddBillerRequest;
import com.bankease.api.dto.request.BillPayRequest;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.BillPaymentResponse;
import com.bankease.api.dto.response.BillerResponse;
import com.bankease.api.dto.response.PagedResponse;
import com.bankease.api.service.BillPayService;
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
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bill Pay", description = "Biller management and utility payment APIs")
public class BillPayController {

    private final BillPayService billPayService;
    private final SecurityUtils securityUtils;

    @GetMapping("/billers")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List registered billers")
    public ResponseEntity<ApiResponse<List<BillerResponse>>> getBillers() {
        Long userId = securityUtils.getCurrentUserId();
        List<BillerResponse> billers = billPayService.getBillers(userId);
        return ResponseEntity.ok(ApiResponse.success(billers));
    }

    @PostMapping("/billers")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Register new utility biller")
    public ResponseEntity<ApiResponse<BillerResponse>> addBiller(@Valid @RequestBody AddBillerRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        BillerResponse response = billPayService.addBiller(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Biller registered", response));
    }

    @DeleteMapping("/billers/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove registered biller")
    public ResponseEntity<ApiResponse<Void>> deleteBiller(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        billPayService.deleteBiller(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Biller deleted", null));
    }

    @PostMapping("/pay")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Pay utility bill")
    public ResponseEntity<ApiResponse<BillPaymentResponse>> payBill(@Valid @RequestBody BillPayRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        BillPaymentResponse response = billPayService.payBill(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Bill paid successfully", response));
    }

    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get bill payment history")
    public ResponseEntity<ApiResponse<PagedResponse<BillPaymentResponse>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = securityUtils.getCurrentUserId();
        PagedResponse<BillPaymentResponse> history = billPayService.getPaymentHistory(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}

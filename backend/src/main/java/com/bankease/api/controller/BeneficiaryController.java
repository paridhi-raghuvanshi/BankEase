package com.bankease.api.controller;

import com.bankease.api.dto.request.AddBeneficiaryRequest;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.BeneficiaryResponse;
import com.bankease.api.service.BeneficiaryService;
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
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Beneficiaries", description = "Beneficiary management APIs")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all active beneficiaries")
    public ResponseEntity<ApiResponse<List<BeneficiaryResponse>>> getBeneficiaries() {
        Long userId = securityUtils.getCurrentUserId();
        List<BeneficiaryResponse> list = beneficiaryService.getBeneficiaries(userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add a new beneficiary")
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> addBeneficiary(@Valid @RequestBody AddBeneficiaryRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        BeneficiaryResponse response = beneficiaryService.addBeneficiary(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Beneficiary added successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete beneficiary by ID")
    public ResponseEntity<ApiResponse<Void>> deleteBeneficiary(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        beneficiaryService.deleteBeneficiary(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Beneficiary deleted successfully", null));
    }
}

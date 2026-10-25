package com.bankease.api.controller;

import com.bankease.api.dto.request.BeneficiaryRequest;
import com.bankease.api.dto.response.ApiResponse;
import com.bankease.api.dto.response.BeneficiaryResponse;
import com.bankease.api.repository.UserRepository;
import com.bankease.api.service.BeneficiaryService;
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
 * REST controller for managing beneficiaries in BankEase.
 *
 * <p>Allows authenticated users to list, add, and remove saved beneficiaries
 * for faster fund transfers.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Beneficiaries", description = "Endpoints for managing saved transfer beneficiaries.")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Returns all saved beneficiaries for the currently authenticated user.
     *
     * @return a list of {@link BeneficiaryResponse} objects
     */
    @Operation(summary = "Get beneficiaries", description = "Retrieves all saved beneficiaries belonging to the currently authenticated user.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<BeneficiaryResponse>>> getBeneficiaries() {
        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.debug("Fetching beneficiaries for userId={}", userId);
        List<BeneficiaryResponse> beneficiaries = beneficiaryService.getBeneficiariesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Beneficiaries retrieved successfully.", beneficiaries));
    }

    /**
     * Adds a new beneficiary for the currently authenticated user.
     *
     * @param request the beneficiary details including account number, name, and bank information
     * @return the newly created {@link BeneficiaryResponse}
     */
    @Operation(summary = "Add beneficiary", description = "Saves a new beneficiary to the user's beneficiary list for faster future transfers.")
    @PostMapping
    public ResponseEntity<ApiResponse<BeneficiaryResponse>> addBeneficiary(
            @Valid @RequestBody BeneficiaryRequest request) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.info("Adding beneficiary for userId={} | accountNumber={}", userId, request.getAccountNumber());
        BeneficiaryResponse beneficiary = beneficiaryService.addBeneficiary(request, userId);
        log.info("Beneficiary added successfully | id={} userId={}", beneficiary.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Beneficiary added successfully.", beneficiary));
    }

    /**
     * Removes a saved beneficiary by its ID.
     *
     * <p>The service layer validates that the beneficiary belongs to the currently
     * authenticated user before deletion.</p>
     *
     * @param id the database ID of the beneficiary to delete
     * @return a success acknowledgement message
     */
    @Operation(summary = "Delete beneficiary", description = "Removes a saved beneficiary from the user's list. The user must own the beneficiary record.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBeneficiary(
            @Parameter(description = "The ID of the beneficiary to delete", required = true)
            @PathVariable Long id) {

        Long userId = securityUtils.getCurrentUserId(userRepository);
        log.info("Deleting beneficiary id={} for userId={}", id, userId);
        beneficiaryService.deleteBeneficiary(id, userId);
        log.info("Beneficiary id={} deleted successfully for userId={}", id, userId);
        return ResponseEntity.ok(ApiResponse.success("Beneficiary deleted successfully.", null));
    }
}

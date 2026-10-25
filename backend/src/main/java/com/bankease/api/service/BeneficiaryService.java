package com.bankease.api.service;

import com.bankease.api.dto.request.AddBeneficiaryRequest;
import com.bankease.api.dto.response.BeneficiaryResponse;
import com.bankease.api.entity.Beneficiary;
import com.bankease.api.exception.DuplicateBeneficiaryException;
import com.bankease.api.exception.ResourceNotFoundException;
import com.bankease.api.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing beneficiary (payee) records associated with a user.
 * <p>
 * Beneficiaries are logically deleted via a status flag; no records are ever
 * physically removed from the database so that historical transaction references
 * remain intact.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class BeneficiaryService {

    private static final Logger log = LoggerFactory.getLogger(BeneficiaryService.class);

    private final BeneficiaryRepository beneficiaryRepository;

    /**
     * Retrieves all active beneficiaries for the given user.
     *
     * @param userId the ID of the owning user
     * @return list of active {@link BeneficiaryResponse} DTOs
     */
    public List<BeneficiaryResponse> getBeneficiaries(Long userId) {
        log.debug("Fetching beneficiaries for userId={}", userId);
        return beneficiaryRepository.findByUserIdAndStatus(userId, Beneficiary.BeneficiaryStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Adds a new beneficiary for a user after checking for duplicates.
     * <p>
     * A duplicate is defined as an existing active beneficiary with the same account
     * number belonging to the same user.
     * </p>
     *
     * @param request the beneficiary details supplied by the client
     * @param userId  the ID of the owning user
     * @return the persisted {@link BeneficiaryResponse}
     * @throws DuplicateBeneficiaryException if the account number already exists for this user
     */
    public BeneficiaryResponse addBeneficiary(AddBeneficiaryRequest request, Long userId) {
        log.info("Adding beneficiary '{}' for userId={}", request.getAccountNumber(), userId);

        boolean duplicate = beneficiaryRepository
                .existsByUserIdAndAccountNumberAndStatus(
                        userId,
                        request.getAccountNumber(),
                        Beneficiary.BeneficiaryStatus.ACTIVE);

        if (duplicate) {
            throw new DuplicateBeneficiaryException(
                    "Beneficiary with account number " + request.getAccountNumber()
                            + " already exists.");
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .userId(userId)
                .name(request.getName())
                .accountNumber(request.getAccountNumber())
                .ifscCode(request.getIfscCode())
                .bankName(request.getBankName())
                .nickname(request.getNickname())
                .status(Beneficiary.BeneficiaryStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Beneficiary saved = beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary saved with id={} for userId={}", saved.getId(), userId);
        return mapToResponse(saved);
    }

    /**
     * Soft-deletes a beneficiary by setting its status to {@code DELETED}.
     * <p>
     * The operation is scoped to the provided {@code userId} so that one user cannot
     * delete another user's beneficiary.
     * </p>
     *
     * @param id     the ID of the beneficiary to delete
     * @param userId the ID of the requesting user
     * @throws ResourceNotFoundException if no active beneficiary with the given ID belongs to the user
     */
    public void deleteBeneficiary(Long id, Long userId) {
        log.info("Soft-deleting beneficiary id={} for userId={}", id, userId);

        Beneficiary beneficiary = beneficiaryRepository
                .findByIdAndUserIdAndStatus(id, userId, Beneficiary.BeneficiaryStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Beneficiary not found with id: " + id));

        beneficiary.setStatus(Beneficiary.BeneficiaryStatus.DELETED);
        beneficiary.setDeletedAt(LocalDateTime.now());
        beneficiaryRepository.save(beneficiary);
        log.info("Beneficiary id={} soft-deleted for userId={}", id, userId);
    }

    // -------------------------------------------------------------------------
    // Private mapping helpers
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link Beneficiary} entity to its corresponding response DTO.
     *
     * @param b the entity to map
     * @return populated {@link BeneficiaryResponse}
     */
    private BeneficiaryResponse mapToResponse(Beneficiary b) {
        return BeneficiaryResponse.builder()
                .id(b.getId())
                .userId(b.getUserId())
                .name(b.getName())
                .accountNumber(b.getAccountNumber())
                .ifscCode(b.getIfscCode())
                .bankName(b.getBankName())
                .nickname(b.getNickname())
                .status(b.getStatus().name())
                .createdAt(b.getCreatedAt())
                .build();
    }
}

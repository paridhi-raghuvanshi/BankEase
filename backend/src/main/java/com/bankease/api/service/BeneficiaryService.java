package com.bankease.api.service;

import com.bankease.api.dto.request.AddBeneficiaryRequest;
import com.bankease.api.dto.response.BeneficiaryResponse;
import com.bankease.api.entity.Beneficiary;
import com.bankease.api.entity.User;
import com.bankease.api.exception.DuplicateBeneficiaryException;
import com.bankease.api.exception.ResourceNotFoundException;
import com.bankease.api.repository.BeneficiaryRepository;
import com.bankease.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<BeneficiaryResponse> getBeneficiaries(Long userId) {
        return beneficiaryRepository.findByUserIdAndStatus(userId, Beneficiary.BeneficiaryStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BeneficiaryResponse addBeneficiary(AddBeneficiaryRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (beneficiaryRepository.existsByUserIdAndAccountNumber(userId, request.getAccountNumber())) {
            throw new DuplicateBeneficiaryException("Beneficiary with this account number already exists");
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .user(user)
                .nickname(request.getNickname())
                .accountHolderName(request.getAccountHolderName())
                .accountNumber(request.getAccountNumber())
                .ifscCode(request.getIfscCode())
                .bankName(request.getBankName())
                .branchName(request.getBranchName())
                .status(Beneficiary.BeneficiaryStatus.ACTIVE)
                .verified(true)
                .build();

        beneficiary = beneficiaryRepository.save(beneficiary);
        auditLogService.log(userId, user.getEmail(), "ADD_BENEFICIARY", "Added beneficiary: " + request.getNickname(), null);

        return mapToResponse(beneficiary);
    }

    @Transactional
    public void deleteBeneficiary(Long id, Long userId) {
        Beneficiary beneficiary = beneficiaryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Beneficiary not found"));

        beneficiary.setStatus(Beneficiary.BeneficiaryStatus.DELETED);
        beneficiaryRepository.save(beneficiary);
    }

    private BeneficiaryResponse mapToResponse(Beneficiary b) {
        return BeneficiaryResponse.builder()
                .id(b.getId())
                .nickname(b.getNickname())
                .accountHolderName(b.getAccountHolderName())
                .accountNumber(b.getAccountNumber())
                .ifscCode(b.getIfscCode())
                .bankName(b.getBankName())
                .branchName(b.getBranchName())
                .status(b.getStatus().name())
                .verified(b.isVerified())
                .createdAt(b.getCreatedAt())
                .build();
    }
}

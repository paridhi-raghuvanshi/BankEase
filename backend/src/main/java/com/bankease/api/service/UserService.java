package com.bankease.api.service;

import com.bankease.api.dto.response.AuditLogResponse;
import com.bankease.api.dto.response.PagedResponse;
import com.bankease.api.dto.response.UserSummaryResponse;
import com.bankease.api.entity.AuditLog;
import com.bankease.api.entity.User;
import com.bankease.api.exception.ResourceNotFoundException;
import com.bankease.api.repository.AuditLogRepository;
import com.bankease.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return UserSummaryResponse.builder()
                .id(user.getId())
                .customerId(user.getCustomerId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> getUserAuditLogs(Long userId, int page, int size) {
        Page<AuditLog> logs = auditLogRepository.findByUserId(userId, PageRequest.of(page, size));
        return PagedResponse.<AuditLogResponse>builder()
                .content(logs.getContent().stream().map(l -> AuditLogResponse.builder()
                        .id(l.getId())
                        .userId(l.getUserId())
                        .userEmail(l.getUserEmail())
                        .action(l.getAction())
                        .description(l.getDescription())
                        .entityType(l.getEntityType())
                        .entityId(l.getEntityId())
                        .ipAddress(l.getIpAddress())
                        .result(l.getResult().name())
                        .createdAt(l.getCreatedAt())
                        .build()).collect(Collectors.toList()))
                .page(logs.getNumber())
                .size(logs.getSize())
                .totalElements(logs.getTotalElements())
                .totalPages(logs.getTotalPages())
                .last(logs.isLast())
                .build();
    }
}

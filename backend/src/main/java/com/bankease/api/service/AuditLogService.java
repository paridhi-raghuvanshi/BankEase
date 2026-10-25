package com.bankease.api.service;

import com.bankease.api.entity.AuditLog;
import com.bankease.api.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service responsible for persisting audit log entries asynchronously.
 * <p>
 * All public methods are annotated with {@link Async} so they execute in a
 * separate thread pool and never block the calling request. Callers should
 * therefore not rely on any return value or assume the entry has been persisted
 * before the method returns.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    /**
     * Records a full audit event with all contextual metadata.
     *
     * @param userId      the ID of the acting user (may be {@code null} for anonymous events)
     * @param email       the email of the acting user
     * @param action      a short action label, e.g. {@code "LOGIN"} or {@code "TRANSFER"}
     * @param description a human-readable description of what happened
     * @param entityType  the domain entity involved, e.g. {@code "Account"} or {@code "Transaction"}
     * @param entityId    the identifier of the entity involved
     * @param ip          the originating IP address of the request
     * @param userAgent   the HTTP User-Agent header value
     * @param result      whether the action succeeded or failed
     */
    @Async
    public void log(Long userId,
                    String email,
                    String action,
                    String description,
                    String entityType,
                    String entityId,
                    String ip,
                    String userAgent,
                    AuditLog.AuditResult result) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(email)
                    .action(action)
                    .description(description)
                    .entityType(entityType)
                    .entityId(entityId)
                    .ipAddress(ip)
                    .userAgent(userAgent)
                    .result(result)
                    .createdAt(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: action={}, userId={}, result={}", action, userId, result);
        } catch (Exception ex) {
            // Never let audit failures propagate to the caller
            log.error("Failed to persist audit log entry: action={}, userId={}, error={}",
                    action, userId, ex.getMessage(), ex);
        }
    }

    /**
     * Convenience overload that records a successful audit event without entity or browser metadata.
     * <p>
     * This overload always uses {@link AuditLog.AuditResult#SUCCESS} as the result.
     * </p>
     *
     * @param userId      the ID of the acting user
     * @param email       the email of the acting user
     * @param action      a short action label
     * @param description a human-readable description of what happened
     * @param ip          the originating IP address of the request
     */
    @Async
    public void log(Long userId,
                    String email,
                    String action,
                    String description,
                    String ip) {
        log(userId, email, action, description, null, null, ip, null, AuditLog.AuditResult.SUCCESS);
    }
}

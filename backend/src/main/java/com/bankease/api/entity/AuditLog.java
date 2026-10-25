package com.bankease.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_action", columnList = "action")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 150)
    private String userEmail;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String entityType;

    @Column(length = 50)
    private String entityId;

    @Column(length = 50)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditResult result = AuditResult.SUCCESS;

    @Column(length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum AuditResult {
        SUCCESS, FAILURE, PARTIAL
    }
}

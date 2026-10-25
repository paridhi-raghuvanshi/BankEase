package com.bankease.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billers", indexes = {
        @Index(name = "idx_biller_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Biller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String billerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillerCategory category;

    @Column(nullable = false, length = 100)
    private String consumerId;

    @Column(length = 200)
    private String consumerName;

    @Column(length = 100)
    private String nickname;

    @Column(precision = 15, scale = 2)
    private BigDecimal dueAmount;

    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillerStatus status = BillerStatus.ACTIVE;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum BillerCategory {
        ELECTRICITY, WATER, GAS, BROADBAND, MOBILE_POSTPAID, MOBILE_PREPAID,
        DTH, INSURANCE, CREDIT_CARD, LOAN_EMI, MUNICIPAL_TAX, OTHER
    }

    public enum BillerStatus {
        ACTIVE, INACTIVE, DELETED
    }
}

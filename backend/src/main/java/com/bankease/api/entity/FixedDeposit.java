package com.bankease.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fixed_deposits", indexes = {
        @Index(name = "idx_fd_number", columnList = "fd_number", unique = true),
        @Index(name = "idx_fd_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FixedDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fd_number", nullable = false, unique = true, length = 20)
    private String fdNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private int tenureMonths;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate maturityDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maturityAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterestPayoutType interestPayoutType = InterestPayoutType.ON_MATURITY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FDStatus status = FDStatus.ACTIVE;

    @Column(precision = 15, scale = 2)
    private BigDecimal prematureWithdrawalAmount;

    @Column(precision = 10, scale = 4)
    private BigDecimal penaltyRate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum InterestPayoutType {
        MONTHLY, QUARTERLY, ANNUALLY, ON_MATURITY
    }

    public enum FDStatus {
        ACTIVE, MATURED, PREMATURE_CLOSED, CLOSED
    }
}

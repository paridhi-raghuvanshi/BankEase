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
@Table(name = "recurring_deposits", indexes = {
        @Index(name = "idx_rd_number", columnList = "rd_number", unique = true),
        @Index(name = "idx_rd_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rd_number", nullable = false, unique = true, length = 20)
    private String rdNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyInstallment;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private int tenureMonths;

    @Column(nullable = false)
    private int installmentsPaid = 0;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate maturityDate;

    private LocalDate nextInstallmentDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDeposited = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maturityAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RDStatus status = RDStatus.ACTIVE;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum RDStatus {
        ACTIVE, MATURED, PREMATURE_CLOSED, CLOSED
    }
}

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
@Table(name = "loans", indexes = {
        @Index(name = "idx_loan_number", columnList = "loan_number", unique = true),
        @Index(name = "idx_loan_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_number", nullable = false, unique = true, length = 20)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanType loanType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private int tenureMonths;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal emiAmount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private LocalDate nextEmiDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status = LoanStatus.APPLIED;

    @Column(length = 500)
    private String purpose;

    @Column(length = 1000)
    private String remarks;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum LoanType {
        HOME, PERSONAL, CAR, EDUCATION, GOLD, BUSINESS
    }

    public enum LoanStatus {
        APPLIED, UNDER_REVIEW, APPROVED, DISBURSED, ACTIVE, CLOSED, REJECTED, NPA
    }
}

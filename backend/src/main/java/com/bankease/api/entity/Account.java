package com.bankease.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_account_number", columnList = "account_number", unique = true),
        @Index(name = "idx_account_user", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 16)
    private String accountNumber;

    @Column(nullable = false, length = 20)
    private String ifscCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(precision = 10, scale = 4)
    private BigDecimal interestRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal dailyTransferLimit = new BigDecimal("100000.00");

    @Column(precision = 15, scale = 2)
    private BigDecimal monthlyTransferLimit = new BigDecimal("1000000.00");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "sourceAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> outgoingTransactions;

    @OneToMany(mappedBy = "destinationAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> incomingTransactions;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum AccountType {
        SAVINGS, CURRENT, SALARY, NRI
    }

    public enum AccountStatus {
        ACTIVE, INACTIVE, FROZEN, CLOSED
    }
}

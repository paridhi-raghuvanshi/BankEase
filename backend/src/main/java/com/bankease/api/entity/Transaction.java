package com.bankease.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_txn_reference", columnList = "reference_number", unique = true),
        @Index(name = "idx_txn_source_account", columnList = "source_account_id"),
        @Index(name = "idx_txn_dest_account", columnList = "destination_account_id"),
        @Index(name = "idx_txn_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_number", nullable = false, unique = true, length = 30)
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionMode mode;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(precision = 15, scale = 2)
    private BigDecimal charges = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 500)
    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(length = 50)
    private String beneficiaryName;

    @Column(length = 16)
    private String beneficiaryAccountNumber;

    @Column(length = 20)
    private String beneficiaryIfsc;

    @Column(precision = 15, scale = 2)
    private BigDecimal balanceAfterTransaction;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    public enum TransactionType {
        DEBIT, CREDIT
    }

    public enum TransactionMode {
        NEFT, RTGS, IMPS, UPI, INTERNAL, BILL_PAYMENT, LOAN_EMI, FD_INTEREST, RD_INSTALLMENT, CHARGE
    }

    public enum TransactionStatus {
        PENDING, SUCCESS, FAILED, REVERSED
    }
}

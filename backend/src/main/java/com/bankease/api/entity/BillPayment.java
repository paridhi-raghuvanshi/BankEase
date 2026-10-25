package com.bankease.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill_payments", indexes = {
        @Index(name = "idx_bill_payment_biller", columnList = "biller_id"),
        @Index(name = "idx_bill_payment_txn", columnList = "transaction_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "biller_id", nullable = false)
    private Biller biller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 50)
    private String billPeriod;

    @Column(length = 30)
    private String confirmationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.SUCCESS;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime paidAt;

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED
    }
}

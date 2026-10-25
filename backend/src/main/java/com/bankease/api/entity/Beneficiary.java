package com.bankease.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries", indexes = {
        @Index(name = "idx_beneficiary_user", columnList = "user_id"),
        @Index(name = "idx_beneficiary_account", columnList = "account_number")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(nullable = false, length = 100)
    private String accountHolderName;

    @Column(nullable = false, length = 16)
    private String accountNumber;

    @Column(nullable = false, length = 20)
    private String ifscCode;

    @Column(nullable = false, length = 100)
    private String bankName;

    @Column(nullable = false, length = 100)
    private String branchName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeneficiaryStatus status = BeneficiaryStatus.ACTIVE;

    private boolean verified = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum BeneficiaryStatus {
        ACTIVE, INACTIVE, DELETED
    }
}

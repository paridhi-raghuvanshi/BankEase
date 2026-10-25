package com.bankease.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_customer_id", columnList = "customer_id", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true, length = 12)
    private String customerId;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 10)
    private String pincode;

    @Column(length = 50)
    private String city;

    @Column(length = 50)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private boolean mfaEnabled = false;

    @Column(length = 100)
    private String mfaSecret;

    private int failedLoginAttempts = 0;

    private LocalDateTime lockedUntil;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Account> accounts = new HashSet<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, SUSPENDED, LOCKED
    }
}

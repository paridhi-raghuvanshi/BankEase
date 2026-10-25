package com.bankease.api.repository;

import com.bankease.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCustomerId(String customerId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.email = :email")
    void incrementFailedAttempts(String email);

    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.email = :email")
    void resetFailedAttempts(String email);

    @Modifying
    @Query("UPDATE User u SET u.status = :status, u.lockedUntil = :lockedUntil WHERE u.email = :email")
    void lockAccount(String email, User.UserStatus status, LocalDateTime lockedUntil);
}

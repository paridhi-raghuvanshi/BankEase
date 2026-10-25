package com.bankease.api.repository;

import com.bankease.api.entity.RecurringDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecurringDepositRepository extends JpaRepository<RecurringDeposit, Long> {

    List<RecurringDeposit> findByUserId(Long userId);

    List<RecurringDeposit> findByUserIdAndStatus(Long userId, RecurringDeposit.RDStatus status);

    Optional<RecurringDeposit> findByRdNumber(String rdNumber);

    Optional<RecurringDeposit> findByIdAndUserId(Long id, Long userId);
}

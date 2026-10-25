package com.bankease.api.repository;

import com.bankease.api.entity.FixedDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FixedDepositRepository extends JpaRepository<FixedDeposit, Long> {

    List<FixedDeposit> findByUserId(Long userId);

    List<FixedDeposit> findByUserIdAndStatus(Long userId, FixedDeposit.FDStatus status);

    Optional<FixedDeposit> findByFdNumber(String fdNumber);

    Optional<FixedDeposit> findByIdAndUserId(Long id, Long userId);
}

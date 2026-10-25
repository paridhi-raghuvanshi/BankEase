package com.bankease.api.repository;

import com.bankease.api.entity.Beneficiary;
import com.bankease.api.entity.Beneficiary.BeneficiaryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByUserIdAndStatus(Long userId, BeneficiaryStatus status);

    Optional<Beneficiary> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndAccountNumber(Long userId, String accountNumber);
}

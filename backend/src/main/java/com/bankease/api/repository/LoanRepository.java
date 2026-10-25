package com.bankease.api.repository;

import com.bankease.api.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserId(Long userId);

    List<Loan> findByUserIdAndStatus(Long userId, Loan.LoanStatus status);

    Optional<Loan> findByLoanNumber(String loanNumber);

    Optional<Loan> findByIdAndUserId(Long id, Long userId);
}

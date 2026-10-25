package com.bankease.api.repository;

import com.bankease.api.entity.Transaction;
import com.bankease.api.entity.Transaction.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId)
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId)
            AND t.createdAt BETWEEN :from AND :to
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByAccountIdAndDateRange(
            Long accountId, LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.sourceAccount.user.id = :userId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.sourceAccount.id = :accountId AND t.status = :status AND t.createdAt >= :from")
    long countByAccountIdAndStatusAndCreatedAtAfter(Long accountId, TransactionStatus status, LocalDateTime from);
}

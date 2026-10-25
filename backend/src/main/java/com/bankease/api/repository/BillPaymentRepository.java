package com.bankease.api.repository;

import com.bankease.api.entity.BillPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {

    @Query("SELECT bp FROM BillPayment bp WHERE bp.biller.user.id = :userId ORDER BY bp.paidAt DESC")
    Page<BillPayment> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT bp FROM BillPayment bp WHERE bp.biller.id = :billerId ORDER BY bp.paidAt DESC")
    Page<BillPayment> findByBillerId(Long billerId, Pageable pageable);
}

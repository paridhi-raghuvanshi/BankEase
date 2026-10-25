package com.bankease.api.repository;

import com.bankease.api.entity.Biller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillerRepository extends JpaRepository<Biller, Long> {

    List<Biller> findByUserIdAndStatus(Long userId, Biller.BillerStatus status);

    Optional<Biller> findByIdAndUserId(Long id, Long userId);
}

package com.treasury.repository;

import com.treasury.domain.PaymentBatch;
import com.treasury.domain.PaymentBatchStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentBatchRepository extends JpaRepository<PaymentBatch, Long> {

    @Override
    @EntityGraph(attributePaths = {"items", "items.payment", "items.payment.payerAccount"})
    List<PaymentBatch> findAll();

    @EntityGraph(attributePaths = {"items", "items.payment", "items.payment.payerAccount"})
    List<PaymentBatch> findByStatusAndScheduledAtLessThanEqual(PaymentBatchStatus status, LocalDateTime scheduledAt);

    @EntityGraph(attributePaths = {"items", "items.payment", "items.payment.payerAccount"})
    Optional<PaymentBatch> findDetailedById(Long id);
}

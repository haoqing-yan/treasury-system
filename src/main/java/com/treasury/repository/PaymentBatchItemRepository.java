package com.treasury.repository;

import com.treasury.domain.PaymentBatchItem;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentBatchItemRepository extends JpaRepository<PaymentBatchItem, Long> {

    @EntityGraph(attributePaths = {"payment", "payment.payerAccount", "batch"})
    Optional<PaymentBatchItem> findWithPaymentById(Long id);
}

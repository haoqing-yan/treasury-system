package com.treasury.repository;

import com.treasury.domain.BankTransaction;
import com.treasury.domain.ReconciliationStatus;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    @Override
    @EntityGraph(attributePaths = {"bankAccount", "matchedPayment"})
    List<BankTransaction> findAll();

    @EntityGraph(attributePaths = {"bankAccount", "matchedPayment"})
    List<BankTransaction> findAllByOrderByTransactionTimeDesc();

    boolean existsByMatchedPaymentId(Long paymentId);

    boolean existsByTransactionNo(String transactionNo);

    long countByReconciliationStatus(ReconciliationStatus status);
}

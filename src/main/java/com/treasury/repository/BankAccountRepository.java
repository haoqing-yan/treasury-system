package com.treasury.repository;

import com.treasury.domain.BankAccount;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    boolean existsByAccountNo(String accountNo);

    List<BankAccount> findAllByOrderByBalanceDesc();
}

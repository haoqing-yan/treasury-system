package com.treasury.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions", uniqueConstraints =
@UniqueConstraint(name = "uk_bank_transaction_no", columnNames = "transaction_no"))
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_no", nullable = false, length = 64)
    private String transactionNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Column(nullable = false)
    private LocalDateTime transactionTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionDirection direction;

    @Column(nullable = false, length = 120)
    private String counterpartyName;

    @Column(length = 64)
    private String counterpartyAccountNo;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 240)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReconciliationStatus reconciliationStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_payment_id")
    private PaymentOrder matchedPayment;

    @Column(length = 32)
    private String matchMethod;

    @Column(length = 240)
    private String matchMessage;

    private LocalDateTime matchedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    protected BankTransaction() {
    }

    public BankTransaction(String transactionNo, BankAccount bankAccount, LocalDateTime transactionTime,
                           TransactionDirection direction, String counterpartyName, String counterpartyAccountNo,
                           BigDecimal amount, String currency, BigDecimal balanceAfter, String purpose) {
        this.transactionNo = transactionNo;
        this.bankAccount = bankAccount;
        this.transactionTime = transactionTime;
        this.direction = direction;
        this.counterpartyName = counterpartyName;
        this.counterpartyAccountNo = counterpartyAccountNo;
        this.amount = amount;
        this.currency = currency;
        this.balanceAfter = balanceAfter;
        this.purpose = purpose;
        this.reconciliationStatus = ReconciliationStatus.UNMATCHED;
        this.createdAt = LocalDateTime.now();
    }

    public void match(PaymentOrder payment, String method, String message) {
        if (reconciliationStatus == ReconciliationStatus.MATCHED) {
            throw new IllegalStateException("该流水已完成匹配");
        }
        this.matchedPayment = payment;
        this.matchMethod = method;
        this.matchMessage = message;
        this.reconciliationStatus = ReconciliationStatus.MATCHED;
        this.matchedAt = LocalDateTime.now();
    }

    public void markException(String message) {
        if (reconciliationStatus == ReconciliationStatus.MATCHED) {
            throw new IllegalStateException("已匹配流水不能标记为异常");
        }
        this.reconciliationStatus = ReconciliationStatus.EXCEPTION;
        this.matchMethod = "MANUAL_REVIEW";
        this.matchMessage = message;
        this.matchedAt = null;
    }

    public Long getId() {
        return id;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public TransactionDirection getDirection() {
        return direction;
    }

    public String getCounterpartyName() {
        return counterpartyName;
    }

    public String getCounterpartyAccountNo() {
        return counterpartyAccountNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getPurpose() {
        return purpose;
    }

    public ReconciliationStatus getReconciliationStatus() {
        return reconciliationStatus;
    }

    public PaymentOrder getMatchedPayment() {
        return matchedPayment;
    }

    public String getMatchMethod() {
        return matchMethod;
    }

    public String getMatchMessage() {
        return matchMessage;
    }

    public LocalDateTime getMatchedAt() {
        return matchedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

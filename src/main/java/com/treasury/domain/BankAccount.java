package com.treasury.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_accounts", uniqueConstraints = @UniqueConstraint(name = "uk_bank_account_no", columnNames = "account_no"))
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountChannel channel;

    @Column(nullable = false, length = 64)
    private String organizationName;

    @Column(nullable = false, length = 80)
    private String bankName;

    @Column(nullable = false, length = 40)
    private String bankCode;

    @Column(nullable = false, length = 100)
    private String accountName;

    @Column(name = "account_no", nullable = false, length = 64)
    private String accountNo;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal availableBalance;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal lowBalanceThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false)
    private LocalDateTime lastSyncTime;

    @Version
    private Long version;

    protected BankAccount() {
    }

    public BankAccount(AccountChannel channel, String organizationName, String bankName,
                       String bankCode, String accountName,
                       String accountNo, String currency, BigDecimal balance, BigDecimal availableBalance,
                       BigDecimal lowBalanceThreshold, AccountType accountType, AccountStatus status) {
        this.channel = channel;
        this.organizationName = organizationName;
        this.bankName = bankName;
        this.bankCode = bankCode;
        this.accountName = accountName;
        this.accountNo = accountNo;
        this.currency = currency;
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.lowBalanceThreshold = lowBalanceThreshold;
        this.accountType = accountType;
        this.status = status;
        this.lastSyncTime = LocalDateTime.now();
    }

    public void update(String organizationName, String bankName, String bankCode, String accountName,
                       String currency, BigDecimal lowBalanceThreshold, AccountType accountType,
                       AccountStatus status) {
        this.organizationName = organizationName;
        this.bankName = bankName;
        this.bankCode = bankCode;
        this.accountName = accountName;
        this.currency = currency;
        this.lowBalanceThreshold = lowBalanceThreshold;
        this.accountType = accountType;
        this.status = status;
    }

    public void synchronizeBalance(BigDecimal balance, BigDecimal availableBalance) {
        this.balance = balance;
        this.availableBalance = availableBalance;
        this.lastSyncTime = LocalDateTime.now();
    }

    public void debit(BigDecimal amount) {
        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("账户可用余额不足");
        }
        this.balance = this.balance.subtract(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
        this.lastSyncTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public AccountChannel getChannel() {
        return channel;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getLowBalanceThreshold() {
        return lowBalanceThreshold;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public LocalDateTime getLastSyncTime() {
        return lastSyncTime;
    }
}

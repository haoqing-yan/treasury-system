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
@Table(name = "payment_orders", uniqueConstraints = @UniqueConstraint(name = "uk_payment_no", columnNames = "payment_no"))
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_no", nullable = false, length = 32)
    private String paymentNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_account_id", nullable = false)
    private BankAccount payerAccount;

    @Column(nullable = false, length = 120)
    private String payeeName;

    @Column(nullable = false, length = 80)
    private String payeeBankName;

    @Column(nullable = false, length = 64)
    private String payeeAccountNo;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 240)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false, length = 64)
    private String applicant;

    @Column(length = 64)
    private String approver;

    @Column(length = 240)
    private String rejectReason;

    @Column(nullable = false)
    private boolean riskFlag;

    @Column(length = 240)
    private String riskMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;

    @Version
    private Long version;

    protected PaymentOrder() {
    }

    public PaymentOrder(String paymentNo, BankAccount payerAccount, String payeeName, String payeeBankName,
                        String payeeAccountNo, BigDecimal amount, String currency, String purpose,
                        String applicant, boolean riskFlag, String riskMessage) {
        this.paymentNo = paymentNo;
        this.payerAccount = payerAccount;
        this.payeeName = payeeName;
        this.payeeBankName = payeeBankName;
        this.payeeAccountNo = payeeAccountNo;
        this.amount = amount;
        this.currency = currency;
        this.purpose = purpose;
        this.applicant = applicant;
        this.status = PaymentStatus.DRAFT;
        this.riskFlag = riskFlag;
        this.riskMessage = riskMessage;
        this.createdAt = LocalDateTime.now();
    }

    public void submit() {
        requireStatus(PaymentStatus.DRAFT, "只有草稿付款可以提交");
        this.status = PaymentStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
        this.rejectReason = null;
    }

    public void approve(String approver) {
        requireStatus(PaymentStatus.PENDING, "只有待审批付款可以通过");
        this.status = PaymentStatus.APPROVED;
        this.approver = approver;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(String approver, String reason) {
        requireStatus(PaymentStatus.PENDING, "只有待审批付款可以驳回");
        this.status = PaymentStatus.REJECTED;
        this.approver = approver;
        this.rejectReason = reason;
    }

    public void markPaid() {
        requireStatus(PaymentStatus.APPROVED, "只有已审批付款可以发送银行");
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void restoreForSeed(PaymentStatus status, String approver, String rejectReason,
                               LocalDateTime submittedAt, LocalDateTime approvedAt, LocalDateTime paidAt) {
        this.status = status;
        this.approver = approver;
        this.rejectReason = rejectReason;
        this.submittedAt = submittedAt;
        this.approvedAt = approvedAt;
        this.paidAt = paidAt;
    }

    private void requireStatus(PaymentStatus required, String message) {
        if (this.status != required) {
            throw new IllegalStateException(message);
        }
    }

    public Long getId() {
        return id;
    }

    public String getPaymentNo() {
        return paymentNo;
    }

    public BankAccount getPayerAccount() {
        return payerAccount;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public String getPayeeBankName() {
        return payeeBankName;
    }

    public String getPayeeAccountNo() {
        return payeeAccountNo;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPurpose() {
        return purpose;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getApplicant() {
        return applicant;
    }

    public String getApprover() {
        return approver;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public boolean isRiskFlag() {
        return riskFlag;
    }

    public String getRiskMessage() {
        return riskMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}

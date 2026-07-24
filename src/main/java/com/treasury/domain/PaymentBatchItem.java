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

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_batch_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_batch_payment", columnNames = "payment_order_id"))
public class PaymentBatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private PaymentBatch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_order_id", nullable = false)
    private PaymentOrder payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentBatchItemStatus status;

    @Column(nullable = false, length = 64)
    private String requestId;

    @Column(length = 240)
    private String failureReason;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Version
    private Long version;

    protected PaymentBatchItem() {
    }

    PaymentBatchItem(PaymentBatch batch, PaymentOrder payment) {
        this.batch = batch;
        this.payment = payment;
        this.status = PaymentBatchItemStatus.READY;
        this.requestId = payment.getPaymentNo() + "-B01";
    }

    public void start() {
        if (status != PaymentBatchItemStatus.READY) {
            throw new IllegalStateException("批次明细状态不允许执行");
        }
        status = PaymentBatchItemStatus.PROCESSING;
        startedAt = LocalDateTime.now();
    }

    public void succeed() {
        status = PaymentBatchItemStatus.SUCCESS;
        completedAt = LocalDateTime.now();
        failureReason = null;
    }

    public void fail(String reason) {
        status = PaymentBatchItemStatus.FAILED;
        completedAt = LocalDateTime.now();
        failureReason = reason == null ? "渠道执行失败" : reason.substring(0, Math.min(reason.length(), 240));
    }

    public Long getId() { return id; }
    public PaymentBatch getBatch() { return batch; }
    public PaymentOrder getPayment() { return payment; }
    public PaymentBatchItemStatus getStatus() { return status; }
    public String getRequestId() { return requestId; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}

package com.treasury.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_batches",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_batch_no", columnNames = "batch_no"))
public class PaymentBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_no", nullable = false, length = 32)
    private String batchNo;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentBatchStatus status;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private int totalCount;

    @Column(nullable = false)
    private int successCount;

    @Column(nullable = false)
    private int failedCount;

    @Column(nullable = false, length = 64)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id asc")
    private List<PaymentBatchItem> items = new ArrayList<>();

    @Version
    private Long version;

    protected PaymentBatch() {
    }

    public PaymentBatch(String batchNo, LocalDateTime scheduledAt, String createdBy) {
        this.batchNo = batchNo;
        this.scheduledAt = scheduledAt;
        this.createdBy = createdBy;
        this.status = PaymentBatchStatus.READY;
        this.totalAmount = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    public void addPayment(PaymentOrder payment) {
        PaymentBatchItem item = new PaymentBatchItem(this, payment);
        items.add(item);
        totalCount++;
        totalAmount = totalAmount.add(payment.getAmount());
    }

    public void start() {
        if (status != PaymentBatchStatus.READY) {
            throw new IllegalStateException("只有待执行批次可以启动");
        }
        status = PaymentBatchStatus.PROCESSING;
        startedAt = LocalDateTime.now();
    }

    public void finish(int successes, int failures) {
        this.successCount = successes;
        this.failedCount = failures;
        this.completedAt = LocalDateTime.now();
        this.status = failures == 0 ? PaymentBatchStatus.COMPLETED
                : successes == 0 ? PaymentBatchStatus.FAILED : PaymentBatchStatus.PARTIAL_FAILED;
    }

    public Long getId() { return id; }
    public String getBatchNo() { return batchNo; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public PaymentBatchStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getTotalCount() { return totalCount; }
    public int getSuccessCount() { return successCount; }
    public int getFailedCount() { return failedCount; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public List<PaymentBatchItem> getItems() { return items; }
}

package com.treasury.service;

import com.treasury.domain.PaymentBatch;
import com.treasury.domain.PaymentBatchItem;
import com.treasury.domain.PaymentBatchItemStatus;
import com.treasury.repository.PaymentBatchItemRepository;
import com.treasury.repository.PaymentBatchRepository;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentBatchStateService {

    private final PaymentBatchRepository batchRepository;
    private final PaymentBatchItemRepository itemRepository;
    private final AuditService auditService;

    public PaymentBatchStateService(PaymentBatchRepository batchRepository,
                                    PaymentBatchItemRepository itemRepository,
                                    AuditService auditService) {
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.auditService = auditService;
    }

    @Transactional
    public List<Long> start(Long batchId, String username) {
        PaymentBatch batch = batchRepository.findDetailedById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("付款批次不存在"));
        batch.start();
        auditService.record(username, "START", "PAYMENT_BATCH", batchId.toString(),
                "启动付款批次 " + batch.getBatchNo());
        return batch.getItems().stream().map(PaymentBatchItem::getId).toList();
    }

    @Transactional
    public void markItemFailed(Long itemId, String reason, String username) {
        PaymentBatchItem item = itemRepository.findWithPaymentById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("付款批次明细不存在"));
        item.fail(reason);
        item.getPayment().markBatchFailed();
        auditService.record(username, "BATCH_FAILED", "PAYMENT", item.getPayment().getId().toString(),
                "批次付款执行失败：" + item.getPayment().getPaymentNo() + "，" + reason);
    }

    @Transactional
    public void finish(Long batchId, String username) {
        PaymentBatch batch = batchRepository.findDetailedById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("付款批次不存在"));
        int successes = (int) batch.getItems().stream()
                .filter(item -> item.getStatus() == PaymentBatchItemStatus.SUCCESS).count();
        int failures = (int) batch.getItems().stream()
                .filter(item -> item.getStatus() == PaymentBatchItemStatus.FAILED).count();
        batch.finish(successes, failures);
        auditService.record(username, "COMPLETE", "PAYMENT_BATCH", batchId.toString(),
                "付款批次执行完成：成功 " + successes + " 笔，失败 " + failures + " 笔");
    }
}

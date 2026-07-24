package com.treasury.service;

import com.treasury.domain.PaymentBatch;
import com.treasury.domain.PaymentBatchItem;
import com.treasury.domain.PaymentOrder;
import com.treasury.domain.PaymentStatus;
import com.treasury.dto.PaymentBatchDtos;
import com.treasury.repository.PaymentBatchRepository;
import com.treasury.repository.PaymentOrderRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentBatchService {

    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentBatchRepository batchRepository;
    private final PaymentOrderRepository paymentRepository;
    private final AuditService auditService;

    public PaymentBatchService(PaymentBatchRepository batchRepository, PaymentOrderRepository paymentRepository,
                               AuditService auditService) {
        this.batchRepository = batchRepository;
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
    }

    @PreAuthorize("hasAuthority('payment:batch')")
    @Transactional(readOnly = true)
    public List<PaymentBatchDtos.Response> list() {
        return batchRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('payment:batch')")
    @Transactional
    public PaymentBatchDtos.Response create(PaymentBatchDtos.CreateRequest request, String username) {
        List<Long> distinctIds = request.paymentIds().stream().distinct().toList();
        if (distinctIds.size() != request.paymentIds().size()) {
            throw new IllegalArgumentException("付款单不能重复加入批次");
        }
        List<PaymentOrder> payments = paymentRepository.findAllById(distinctIds);
        if (payments.size() != distinctIds.size()) {
            throw new IllegalArgumentException("部分付款单不存在");
        }
        if (payments.stream().anyMatch(payment -> payment.getStatus() != PaymentStatus.APPROVED)) {
            throw new IllegalStateException("只有已审批付款可以加入批次");
        }

        PaymentBatch batch = new PaymentBatch(nextBatchNo(), request.scheduledAt(), username);
        payments.stream()
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .forEach(payment -> {
                    payment.queue();
                    batch.addPayment(payment);
                });
        batchRepository.save(batch);
        auditService.record(username, "CREATE", "PAYMENT_BATCH", batch.getId().toString(),
                "创建付款批次 " + batch.getBatchNo() + "，共 " + batch.getTotalCount() + " 笔");
        return toResponse(batch);
    }

    @Transactional(readOnly = true)
    public PaymentBatchDtos.Response getResponse(Long id) {
        PaymentBatch batch = batchRepository.findDetailedById(id)
                .orElseThrow(() -> new IllegalArgumentException("付款批次不存在"));
        return toResponse(batch);
    }

    PaymentBatchDtos.Response toResponse(PaymentBatch batch) {
        return new PaymentBatchDtos.Response(
                batch.getId(), batch.getBatchNo(), batch.getScheduledAt(), batch.getStatus(),
                batch.getTotalAmount(), batch.getTotalCount(), batch.getSuccessCount(), batch.getFailedCount(),
                batch.getCreatedBy(), batch.getCreatedAt(), batch.getStartedAt(), batch.getCompletedAt(),
                batch.getItems().stream().map(this::toItemResponse).toList()
        );
    }

    private PaymentBatchDtos.ItemResponse toItemResponse(PaymentBatchItem item) {
        PaymentOrder payment = item.getPayment();
        return new PaymentBatchDtos.ItemResponse(
                item.getId(), payment.getId(), payment.getPaymentNo(),
                payment.getPayerAccount().getAccountName(), payment.getPayeeName(), payment.getAmount(),
                payment.getCurrency(), item.getStatus(), item.getRequestId(), item.getFailureReason()
        );
    }

    private String nextBatchNo() {
        return "PC" + LocalDateTime.now().format(NUMBER_TIME) + String.format("%04d", RANDOM.nextInt(10_000));
    }
}

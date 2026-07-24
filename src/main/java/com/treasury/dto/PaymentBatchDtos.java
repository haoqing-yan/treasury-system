package com.treasury.dto;

import com.treasury.domain.PaymentBatchItemStatus;
import com.treasury.domain.PaymentBatchStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class PaymentBatchDtos {
    private PaymentBatchDtos() {
    }

    public record CreateRequest(
            @NotEmpty @Size(max = 200) List<@NotNull Long> paymentIds,
            @NotNull @FutureOrPresent LocalDateTime scheduledAt
    ) {
    }

    public record ItemResponse(
            Long id,
            Long paymentId,
            String paymentNo,
            String payerAccountName,
            String payeeName,
            BigDecimal amount,
            String currency,
            PaymentBatchItemStatus status,
            String requestId,
            String failureReason
    ) {
    }

    public record Response(
            Long id,
            String batchNo,
            LocalDateTime scheduledAt,
            PaymentBatchStatus status,
            BigDecimal totalAmount,
            int totalCount,
            int successCount,
            int failedCount,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime startedAt,
            LocalDateTime completedAt,
            List<ItemResponse> items
    ) {
    }
}

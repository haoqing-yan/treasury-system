package com.treasury.dto;

import com.treasury.domain.AccountChannel;
import com.treasury.domain.ReconciliationStatus;
import com.treasury.domain.TransactionDirection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ReconciliationDtos {
    private ReconciliationDtos() {
    }

    public record Summary(
            long totalCount,
            long matchedCount,
            long unmatchedCount,
            long exceptionCount,
            BigDecimal matchedAmount,
            BigDecimal unmatchedAmount
    ) {
    }

    public record Response(
            Long id,
            String transactionNo,
            Long bankAccountId,
            AccountChannel channel,
            String bankAccountName,
            String bankAccountNoMasked,
            LocalDateTime transactionTime,
            TransactionDirection direction,
            String counterpartyName,
            String counterpartyAccountNoMasked,
            BigDecimal amount,
            String currency,
            BigDecimal balanceAfter,
            String purpose,
            ReconciliationStatus reconciliationStatus,
            Long matchedPaymentId,
            String matchedPaymentNo,
            String matchMethod,
            String matchMessage,
            LocalDateTime matchedAt
    ) {
    }

    public record ManualMatchRequest(@NotNull Long paymentId) {
    }

    public record ExceptionRequest(@NotBlank @Size(max = 240) String reason) {
    }

    public record AutoMatchResult(int matchedCount, Summary summary) {
    }
}

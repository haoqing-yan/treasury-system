package com.treasury.service;

import com.treasury.domain.PaymentBatchStatus;
import com.treasury.dto.PaymentBatchDtos;
import com.treasury.repository.PaymentBatchRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class PaymentBatchExecutionService {

    private final PaymentBatchRepository batchRepository;
    private final PaymentBatchStateService stateService;
    private final PaymentBatchItemExecutor itemExecutor;
    private final PaymentBatchService batchService;
    private final Executor paymentExecutor;

    public PaymentBatchExecutionService(PaymentBatchRepository batchRepository,
                                        PaymentBatchStateService stateService,
                                        PaymentBatchItemExecutor itemExecutor,
                                        PaymentBatchService batchService,
                                        @Qualifier("paymentExecutor") Executor paymentExecutor) {
        this.batchRepository = batchRepository;
        this.stateService = stateService;
        this.itemExecutor = itemExecutor;
        this.batchService = batchService;
        this.paymentExecutor = paymentExecutor;
    }

    @PreAuthorize("hasAuthority('payment:batch')")
    public PaymentBatchDtos.Response executeManually(Long batchId, String username) {
        return execute(batchId, username);
    }

    public PaymentBatchDtos.Response execute(Long batchId, String username) {
        List<Long> itemIds = stateService.start(batchId, username);
        List<CompletableFuture<Void>> tasks = itemIds.stream()
                .map(itemId -> CompletableFuture.runAsync(() -> executeItem(itemId, username), paymentExecutor))
                .toList();
        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();
        stateService.finish(batchId, username);
        return batchService.getResponse(batchId);
    }

    public void executeDueBatches() {
        batchRepository.findByStatusAndScheduledAtLessThanEqual(PaymentBatchStatus.READY, LocalDateTime.now())
                .forEach(batch -> execute(batch.getId(), "system"));
    }

    private void executeItem(Long itemId, String username) {
        try {
            itemExecutor.execute(itemId, username);
        } catch (RuntimeException exception) {
            String reason = exception.getMessage() == null ? "渠道执行失败" : exception.getMessage();
            stateService.markItemFailed(itemId, reason, username);
        }
    }
}

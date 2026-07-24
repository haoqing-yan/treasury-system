package com.treasury.config;

import com.treasury.service.PaymentBatchExecutionService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentBatchScheduler {

    private final PaymentBatchExecutionService executionService;

    public PaymentBatchScheduler(PaymentBatchExecutionService executionService) {
        this.executionService = executionService;
    }

    @Scheduled(fixedDelayString = "${treasury.payment.batch-scan-delay:30000}")
    public void executeDueBatches() {
        executionService.executeDueBatches();
    }
}

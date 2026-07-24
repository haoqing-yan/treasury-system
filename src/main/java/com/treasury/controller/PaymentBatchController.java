package com.treasury.controller;

import com.treasury.dto.PaymentBatchDtos;
import com.treasury.service.PaymentBatchExecutionService;
import com.treasury.service.PaymentBatchService;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-batches")
public class PaymentBatchController {

    private final PaymentBatchService batchService;
    private final PaymentBatchExecutionService executionService;

    public PaymentBatchController(PaymentBatchService batchService, PaymentBatchExecutionService executionService) {
        this.batchService = batchService;
        this.executionService = executionService;
    }

    @GetMapping
    public List<PaymentBatchDtos.Response> list() {
        return batchService.list();
    }

    @PostMapping
    public PaymentBatchDtos.Response create(@Valid @RequestBody PaymentBatchDtos.CreateRequest request,
                                             Principal principal) {
        return batchService.create(request, principal.getName());
    }

    @PostMapping("/{id}/execute")
    public PaymentBatchDtos.Response execute(@PathVariable Long id, Principal principal) {
        return executionService.executeManually(id, principal.getName());
    }
}

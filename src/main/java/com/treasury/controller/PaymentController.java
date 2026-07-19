package com.treasury.controller;

import com.treasury.dto.PaymentDtos;
import com.treasury.service.PaymentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping
    public List<PaymentDtos.Response> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return service.list(keyword, status);
    }

    @PostMapping
    public PaymentDtos.Response create(@Valid @RequestBody PaymentDtos.CreateRequest request, Principal principal) {
        return service.create(request, principal.getName());
    }

    @PostMapping("/{id}/submit")
    public PaymentDtos.Response submit(@PathVariable Long id, Principal principal) {
        return service.submit(id, principal.getName());
    }

    @PostMapping("/{id}/approve")
    public PaymentDtos.Response approve(@PathVariable Long id, Principal principal) {
        return service.approve(id, principal.getName());
    }

    @PostMapping("/{id}/reject")
    public PaymentDtos.Response reject(@PathVariable Long id,
                                       @Valid @RequestBody PaymentDtos.RejectRequest request,
                                       Principal principal) {
        return service.reject(id, request.reason(), principal.getName());
    }

    @PostMapping("/{id}/execute")
    public PaymentDtos.Response execute(@PathVariable Long id, Principal principal) {
        return service.execute(id, principal.getName());
    }
}

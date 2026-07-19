package com.treasury.controller;

import com.treasury.dto.ReconciliationDtos;
import com.treasury.service.ReconciliationService;
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
@RequestMapping("/api/reconciliations")
public class ReconciliationController {

    private final ReconciliationService service;

    public ReconciliationController(ReconciliationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReconciliationDtos.Response> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return service.list(keyword, status);
    }

    @GetMapping("/summary")
    public ReconciliationDtos.Summary summary() {
        return service.summary();
    }

    @PostMapping("/auto-match")
    public ReconciliationDtos.AutoMatchResult autoMatch(Principal principal) {
        return service.autoMatch(principal.getName());
    }

    @PostMapping("/{id}/match")
    public ReconciliationDtos.Response manualMatch(@PathVariable Long id,
                                                   @Valid @RequestBody ReconciliationDtos.ManualMatchRequest request,
                                                   Principal principal) {
        return service.manualMatch(id, request.paymentId(), principal.getName());
    }

    @PostMapping("/{id}/exception")
    public ReconciliationDtos.Response markException(@PathVariable Long id,
                                                     @Valid @RequestBody ReconciliationDtos.ExceptionRequest request,
                                                     Principal principal) {
        return service.markException(id, request.reason(), principal.getName());
    }
}

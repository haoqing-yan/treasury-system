package com.treasury.controller;

import com.treasury.dto.ExceptionCaseDtos;
import com.treasury.service.ExceptionCaseService;
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
@RequestMapping("/api/exceptions")
public class ExceptionCaseController {

    private final ExceptionCaseService service;

    public ExceptionCaseController(ExceptionCaseService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExceptionCaseDtos.Response> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        return service.list(keyword, category, status, type);
    }

    @GetMapping("/summary")
    public ExceptionCaseDtos.Summary summary() {
        return service.summary();
    }

    @PostMapping("/{id}/claim")
    public ExceptionCaseDtos.Response claim(@PathVariable Long id, Principal principal) {
        return service.claim(id, principal.getName());
    }

    @PostMapping("/{id}/resolve")
    public ExceptionCaseDtos.Response resolve(@PathVariable Long id,
                                              @Valid @RequestBody ExceptionCaseDtos.ResolveRequest request,
                                              Principal principal) {
        return service.resolve(id, request.note(), principal.getName());
    }

    @PostMapping("/{id}/reopen")
    public ExceptionCaseDtos.Response reopen(@PathVariable Long id,
                                             @Valid @RequestBody ExceptionCaseDtos.ReopenRequest request,
                                             Principal principal) {
        return service.reopen(id, request.reason(), principal.getName());
    }
}

package com.treasury.controller;

import com.treasury.dto.CashPlanDtos;
import com.treasury.service.CashPlanService;
import jakarta.validation.Valid;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cash-plans")
public class CashPlanController {

    private final CashPlanService service;

    public CashPlanController(CashPlanService service) {
        this.service = service;
    }

    @GetMapping
    public List<CashPlanDtos.Response> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(from, to);
    }

    @PostMapping
    public CashPlanDtos.Response create(@Valid @RequestBody CashPlanDtos.CreateRequest request,
                                        Principal principal) {
        return service.create(request, principal.getName());
    }
}

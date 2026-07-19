package com.treasury.controller;

import com.treasury.dto.AccountDtos;
import com.treasury.service.AccountService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {

    private final AccountService service;

    public BankAccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<AccountDtos.Response> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status) {
        return service.list(keyword, channel, status);
    }

    @PostMapping
    public AccountDtos.Response create(@Valid @RequestBody AccountDtos.CreateRequest request, Principal principal) {
        return service.create(request, principal.getName());
    }

    @PutMapping("/{id}")
    public AccountDtos.Response update(@PathVariable Long id,
                                       @Valid @RequestBody AccountDtos.UpdateRequest request,
                                       Principal principal) {
        return service.update(id, request, principal.getName());
    }

    @PostMapping("/{id}/sync")
    public AccountDtos.Response sync(@PathVariable Long id,
                                     @Valid @RequestBody AccountDtos.SyncRequest request,
                                     Principal principal) {
        return service.sync(id, request, principal.getName());
    }
}

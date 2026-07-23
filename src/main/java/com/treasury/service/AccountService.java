package com.treasury.service;

import com.treasury.domain.BankAccount;
import com.treasury.domain.AccountChannel;
import com.treasury.dto.AccountDtos;
import com.treasury.repository.BankAccountRepository;

import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final BankAccountRepository repository;
    private final AuditService auditService;

    public AccountService(BankAccountRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AccountDtos.Response> list(String keyword, String channel, String status) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return repository.findAllByOrderByBalanceDesc().stream()
                .filter(account -> normalized.isBlank()
                        || account.getOrganizationName().toLowerCase(Locale.ROOT).contains(normalized)
                        || account.getBankName().toLowerCase(Locale.ROOT).contains(normalized)
                        || account.getAccountName().toLowerCase(Locale.ROOT).contains(normalized)
                        || account.getAccountNo().contains(normalized))
                .filter(account -> channel == null || channel.isBlank() || account.getChannel().name().equals(channel))
                .filter(account -> status == null || status.isBlank() || account.getStatus().name().equals(status))
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('account:manage')")
    @Transactional
    public AccountDtos.Response create(AccountDtos.CreateRequest request, String username) {
        AccountChannel channel = request.channel() == null ? AccountChannel.BANK : request.channel();
        if (repository.existsByAccountNo(request.accountNo())) {
            throw new IllegalStateException("该账号或商户号已存在");
        }
        validateAccountNo(channel, request.accountNo());
        if (request.availableBalance().compareTo(request.balance()) > 0) {
            throw new IllegalArgumentException("可用余额不能大于账面余额");
        }
        BankAccount account = new BankAccount(
                channel,
                request.organizationName(), request.bankName(), request.bankCode(), request.accountName(),
                request.accountNo(), request.currency(), request.balance(), request.availableBalance(),
                request.lowBalanceThreshold(), request.accountType(), request.status()
        );
        repository.save(account);
        auditService.record(username, "CREATE", "BANK_ACCOUNT", account.getId().toString(),
                "新增资金账户：" + account.getAccountName());
        return toResponse(account);
    }

    @PreAuthorize("hasAuthority('account:manage')")
    @Transactional
    public AccountDtos.Response update(Long id, AccountDtos.UpdateRequest request, String username) {
        BankAccount account = get(id);
        account.update(request.organizationName(), request.bankName(), request.bankCode(), request.accountName(),
                request.currency(), request.lowBalanceThreshold(), request.accountType(), request.status());
        auditService.record(username, "UPDATE", "BANK_ACCOUNT", id.toString(),
                "更新资金账户：" + account.getAccountName());
        return toResponse(account);
    }

    @PreAuthorize("hasAuthority('account:manage')")
    @Transactional
    public AccountDtos.Response sync(Long id, AccountDtos.SyncRequest request, String username) {
        if (request.availableBalance().compareTo(request.balance()) > 0) {
            throw new IllegalArgumentException("可用余额不能大于账面余额");
        }
        BankAccount account = get(id);
        account.synchronizeBalance(request.balance(), request.availableBalance());
        auditService.record(username, "SYNC", "BANK_ACCOUNT", id.toString(),
                "手工同步账户余额（演示渠道接口）");
        return toResponse(account);
    }

    @Transactional(readOnly = true)
    public BankAccount get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("资金账户不存在"));
    }

    public AccountDtos.Response toResponse(BankAccount account) {
        return new AccountDtos.Response(
                account.getId(), account.getChannel(), account.getOrganizationName(),
                account.getBankName(), account.getBankCode(),
                account.getAccountName(), MaskingUtils.accountNo(account.getAccountNo()), account.getCurrency(),
                account.getBalance(), account.getAvailableBalance(), account.getLowBalanceThreshold(),
                account.getAccountType(), account.getStatus(),
                account.getAvailableBalance().compareTo(account.getLowBalanceThreshold()) < 0,
                account.getLastSyncTime()
        );
    }

    private void validateAccountNo(AccountChannel channel, String accountNo) {
        if (channel == AccountChannel.BANK && !accountNo.matches("[0-9]{8,32}")) {
            throw new IllegalArgumentException("银行账号须为 8 至 32 位数字");
        }
    }
}

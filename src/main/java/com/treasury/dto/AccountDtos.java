package com.treasury.dto;

import com.treasury.domain.AccountStatus;
import com.treasury.domain.AccountChannel;
import com.treasury.domain.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AccountDtos {
    private AccountDtos() {
    }

    public record CreateRequest(
            AccountChannel channel,
            @NotBlank @Size(max = 64) String organizationName,
            @NotBlank @Size(max = 80) String bankName,
            @NotBlank @Size(max = 40) String bankCode,
            @NotBlank @Size(max = 100) String accountName,
            @NotBlank
            @Pattern(regexp = "[A-Za-z0-9@._-]{6,64}", message = "账号或商户号格式不正确")
            String accountNo,
            @NotBlank @Pattern(regexp = "[A-Z]{3}", message = "币种须为三位大写代码") String currency,
            @NotNull @DecimalMin("0.00") BigDecimal balance,
            @NotNull @DecimalMin("0.00") BigDecimal availableBalance,
            @NotNull @DecimalMin("0.00") BigDecimal lowBalanceThreshold,
            @NotNull AccountType accountType,
            @NotNull AccountStatus status
    ) {
    }

    public record UpdateRequest(
            @NotBlank @Size(max = 64) String organizationName,
            @NotBlank @Size(max = 80) String bankName,
            @NotBlank @Size(max = 40) String bankCode,
            @NotBlank @Size(max = 100) String accountName,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull @DecimalMin("0.00") BigDecimal lowBalanceThreshold,
            @NotNull AccountType accountType,
            @NotNull AccountStatus status
    ) {
    }

    public record SyncRequest(
            @NotNull @DecimalMin("0.00") BigDecimal balance,
            @NotNull @DecimalMin("0.00") BigDecimal availableBalance
    ) {
    }

    public record Response(
            Long id,
            AccountChannel channel,
            String organizationName,
            String bankName,
            String bankCode,
            String accountName,
            String accountNoMasked,
            String currency,
            BigDecimal balance,
            BigDecimal availableBalance,
            BigDecimal lowBalanceThreshold,
            AccountType accountType,
            AccountStatus status,
            boolean lowBalance,
            LocalDateTime lastSyncTime
    ) {
    }
}

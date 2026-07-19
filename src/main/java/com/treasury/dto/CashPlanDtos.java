package com.treasury.dto;

import com.treasury.domain.CashPlanType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CashPlanDtos {
    private CashPlanDtos() {
    }

    public record CreateRequest(
            @NotNull LocalDate planDate,
            @NotNull CashPlanType type,
            @NotBlank @Size(max = 64) String category,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank @Size(max = 64) String organizationName,
            @Size(max = 240) String description
    ) {
    }

    public record Response(
            Long id,
            LocalDate planDate,
            CashPlanType type,
            String category,
            BigDecimal amount,
            String organizationName,
            String description,
            String createdBy,
            LocalDateTime createdAt
    ) {
    }
}

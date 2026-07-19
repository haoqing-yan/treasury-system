package com.treasury.dto;

import com.treasury.domain.AccountChannel;
import com.treasury.domain.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PaymentDtos {
    private PaymentDtos() {
    }

    public record CreateRequest(
            @NotNull Long payerAccountId,
            @NotBlank @Size(max = 120) String payeeName,
            @NotBlank @Size(max = 80) String payeeBankName,
            @NotBlank
            @Pattern(regexp = "[A-Za-z0-9@._-]{6,64}", message = "收款账号或用户标识格式不正确")
            String payeeAccountNo,
            @NotNull @DecimalMin(value = "0.01", message = "付款金额必须大于0") BigDecimal amount,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotBlank @Size(max = 240) String purpose
    ) {
    }

    public record RejectRequest(
            @NotBlank @Size(max = 240) String reason
    ) {
    }

    public record Response(
            Long id,
            String paymentNo,
            Long payerAccountId,
            AccountChannel payerChannel,
            String payerAccountName,
            String payerAccountNoMasked,
            String organizationName,
            String payeeName,
            String payeeBankName,
            String payeeAccountNoMasked,
            BigDecimal amount,
            String currency,
            String purpose,
            PaymentStatus status,
            String applicant,
            String approver,
            String rejectReason,
            boolean riskFlag,
            String riskMessage,
            LocalDateTime createdAt,
            LocalDateTime submittedAt,
            LocalDateTime approvedAt,
            LocalDateTime paidAt
    ) {
    }
}

package com.treasury.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record Overview(
            BigDecimal totalBalance,
            BigDecimal availableBalance,
            long accountCount,
            BigDecimal todayOutflow,
            long pendingPaymentCount,
            BigDecimal nextSevenDaysNet
    ) {
    }

    public record NamedAmount(String name, BigDecimal value) {
    }

    public record CashFlowDay(LocalDate date, BigDecimal inflow, BigDecimal outflow) {
    }

    public record Alert(String level, String title, String description) {
    }

    public record Response(
            Overview overview,
            List<NamedAmount> balanceByBank,
            List<CashFlowDay> cashFlow,
            List<PaymentDtos.Response> recentPayments,
            List<Alert> alerts
    ) {
    }
}

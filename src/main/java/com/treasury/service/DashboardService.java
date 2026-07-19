package com.treasury.service;

import com.treasury.domain.AccountStatus;
import com.treasury.domain.BankAccount;
import com.treasury.domain.CashPlan;
import com.treasury.domain.CashPlanType;
import com.treasury.domain.PaymentOrder;
import com.treasury.domain.PaymentStatus;
import com.treasury.dto.DashboardDtos;
import com.treasury.repository.BankAccountRepository;
import com.treasury.repository.CashPlanRepository;
import com.treasury.repository.PaymentOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final BankAccountRepository accountRepository;
    private final PaymentOrderRepository paymentRepository;
    private final CashPlanRepository cashPlanRepository;
    private final PaymentService paymentService;

    public DashboardService(BankAccountRepository accountRepository, PaymentOrderRepository paymentRepository,
                            CashPlanRepository cashPlanRepository, PaymentService paymentService) {
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.cashPlanRepository = cashPlanRepository;
        this.paymentService = paymentService;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.Response get() {
        List<BankAccount> accounts = accountRepository.findAll();
        List<PaymentOrder> payments = paymentRepository.findAll();
        LocalDate today = LocalDate.now();
        List<CashPlan> plans = cashPlanRepository.findByPlanDateBetweenOrderByPlanDateAsc(today, today.plusDays(6));

        List<BankAccount> activeCnyAccounts = accounts.stream()
                .filter(account -> account.getStatus() != AccountStatus.CLOSED)
                .filter(account -> "CNY".equals(account.getCurrency()))
                .toList();
        BigDecimal totalBalance = sum(activeCnyAccounts.stream().map(BankAccount::getBalance).toList());
        BigDecimal availableBalance = sum(activeCnyAccounts.stream().map(BankAccount::getAvailableBalance).toList());

        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);
        BigDecimal todayOutflow = sum(payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .filter(payment -> payment.getPaidAt() != null
                        && !payment.getPaidAt().isBefore(startOfToday)
                        && !payment.getPaidAt().isAfter(endOfToday))
                .filter(payment -> "CNY".equals(payment.getCurrency()))
                .map(PaymentOrder::getAmount)
                .toList());

        BigDecimal nextSevenDaysNet = plans.stream()
                .map(plan -> plan.getType() == CashPlanType.INFLOW ? plan.getAmount() : plan.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> bankBalances = new LinkedHashMap<>();
        activeCnyAccounts.stream()
                .sorted(Comparator.comparing(BankAccount::getBankName))
                .forEach(account -> bankBalances.merge(account.getBankName(), account.getBalance(), BigDecimal::add));

        List<DashboardDtos.CashFlowDay> cashFlow = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            LocalDate date = today.plusDays(index);
            BigDecimal inflow = sum(plans.stream()
                    .filter(plan -> plan.getPlanDate().equals(date) && plan.getType() == CashPlanType.INFLOW)
                    .map(CashPlan::getAmount).toList());
            BigDecimal outflow = sum(plans.stream()
                    .filter(plan -> plan.getPlanDate().equals(date) && plan.getType() == CashPlanType.OUTFLOW)
                    .map(CashPlan::getAmount).toList());
            cashFlow.add(new DashboardDtos.CashFlowDay(date, inflow, outflow));
        }

        List<DashboardDtos.Alert> alerts = new ArrayList<>();
        accounts.stream()
                .filter(account -> account.getStatus() != AccountStatus.CLOSED)
                .filter(account -> account.getAvailableBalance().compareTo(account.getLowBalanceThreshold()) < 0)
                .forEach(account -> alerts.add(new DashboardDtos.Alert(
                        "warning", "账户余额低于预警线",
                        account.getAccountName() + " 可用余额已低于设定阈值"
                )));
        payments.stream()
                .filter(PaymentOrder::isRiskFlag)
                .filter(payment -> payment.getStatus() != PaymentStatus.REJECTED)
                .limit(2)
                .forEach(payment -> alerts.add(new DashboardDtos.Alert(
                        "danger", "付款重复性风险",
                        payment.getPaymentNo() + "：" + payment.getRiskMessage()
                )));
        long approvedCount = payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.APPROVED).count();
        if (approvedCount > 0) {
            alerts.add(new DashboardDtos.Alert("info", "待执行支付", approvedCount + " 笔已审批付款等待渠道执行"));
        }

        return new DashboardDtos.Response(
                new DashboardDtos.Overview(
                        totalBalance, availableBalance, accounts.size(), todayOutflow,
                        paymentRepository.countByStatus(PaymentStatus.PENDING), nextSevenDaysNet
                ),
                bankBalances.entrySet().stream()
                        .map(entry -> new DashboardDtos.NamedAmount(entry.getKey(), entry.getValue()))
                        .sorted(Comparator.comparing(DashboardDtos.NamedAmount::value).reversed())
                        .toList(),
                cashFlow,
                paymentService.recent(),
                alerts
        );
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

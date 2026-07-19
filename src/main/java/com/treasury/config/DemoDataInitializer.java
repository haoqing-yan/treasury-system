package com.treasury.config;

import com.treasury.domain.AccountStatus;
import com.treasury.domain.AccountChannel;
import com.treasury.domain.AccountType;
import com.treasury.domain.AuditLog;
import com.treasury.domain.BankAccount;
import com.treasury.domain.CashPlan;
import com.treasury.domain.CashPlanType;
import com.treasury.domain.PaymentOrder;
import com.treasury.domain.PaymentStatus;
import com.treasury.repository.AuditLogRepository;
import com.treasury.repository.BankAccountRepository;
import com.treasury.repository.CashPlanRepository;
import com.treasury.repository.PaymentOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
public class DemoDataInitializer implements ApplicationRunner {

    private final BankAccountRepository accountRepository;
    private final PaymentOrderRepository paymentRepository;
    private final CashPlanRepository cashPlanRepository;
    private final AuditLogRepository auditLogRepository;

    public DemoDataInitializer(BankAccountRepository accountRepository, PaymentOrderRepository paymentRepository,
                               CashPlanRepository cashPlanRepository, AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.cashPlanRepository = cashPlanRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountRepository.count() > 0) {
            ensurePaymentPlatformAccounts();
            return;
        }

        BankAccount basic = accountRepository.save(new BankAccount(
                AccountChannel.BANK,
                "华辰控股集团", "中国工商银行", "ICBC", "集团基本账户", "6222021000003847561",
                "CNY", money("62800000"), money("62160000"), money("10000000"),
                AccountType.BASIC, AccountStatus.ACTIVE
        ));
        BankAccount pool = accountRepository.save(new BankAccount(
                AccountChannel.BANK,
                "华辰控股集团", "中国建设银行", "CCB", "集团资金归集户", "6217001000019274018",
                "CNY", money("35200000"), money("35200000"), money("8000000"),
                AccountType.CASH_POOL, AccountStatus.ACTIVE
        ));
        BankAccount payroll = accountRepository.save(new BankAccount(
                AccountChannel.BANK,
                "华辰科技有限公司", "招商银行", "CMB", "员工薪酬专户", "7559010203100573629",
                "CNY", money("4860000"), money("4860000"), money("5000000"),
                AccountType.SPECIAL, AccountStatus.ACTIVE
        ));
        accountRepository.save(new BankAccount(
                AccountChannel.BANK,
                "华辰建设有限公司", "中信银行", "CITIC", "项目监管账户", "8110801013302147821",
                "CNY", money("12500000"), money("3860000"), money("3000000"),
                AccountType.SPECIAL, AccountStatus.RESTRICTED
        ));
        accountRepository.save(new BankAccount(
                AccountChannel.BANK,
                "华辰国际有限公司", "中国银行", "BOC", "美元结算账户", "104100213548900017",
                "USD", money("1320000"), money("1280000"), money("300000"),
                AccountType.SETTLEMENT, AccountStatus.ACTIVE
        ));
        ensurePaymentPlatformAccounts();

        LocalDateTime now = LocalDateTime.now();
        PaymentOrder pending = new PaymentOrder(
                "FK" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "0001",
                basic, "南方智造工程有限公司", "中国农业银行", "6228480402564890012",
                money("1280000"), "CNY", "智能产线一期工程进度款", "operator", false, null
        );
        pending.restoreForSeed(PaymentStatus.PENDING, null, null, now.minusHours(3), null, null);

        PaymentOrder approved = new PaymentOrder(
                "FK" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "0002",
                pool, "联创设备股份有限公司", "交通银行", "6222600710004951687",
                money("486000"), "CNY", "实验设备采购尾款", "operator", false, null
        );
        approved.restoreForSeed(PaymentStatus.APPROVED, "approver", null,
                now.minusHours(7), now.minusHours(2), null);

        PaymentOrder paid = new PaymentOrder(
                "FK" + now.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "0088",
                basic, "恒信企业服务有限公司", "兴业银行", "6229092160810274118",
                money("327600"), "CNY", "园区综合运营服务费", "operator", false, null
        );
        paid.restoreForSeed(PaymentStatus.PAID, "approver", null,
                now.minusDays(1).minusHours(4), now.minusDays(1).minusHours(2), now.minusHours(1));

        PaymentOrder rejected = new PaymentOrder(
                "FK" + now.minusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "0036",
                payroll, "远景咨询工作室", "招商银行", "7559010203100990026",
                money("89000"), "CNY", "战略咨询服务首付款", "operator", true,
                "24小时内存在相同收款账号和金额的付款"
        );
        rejected.restoreForSeed(PaymentStatus.REJECTED, "approver", "合同验收资料不完整",
                now.minusDays(2).minusHours(5), null, null);
        paymentRepository.saveAll(List.of(pending, approved, paid, rejected));

        LocalDate today = LocalDate.now();
        cashPlanRepository.saveAll(List.of(
                plan(today, CashPlanType.INFLOW, "销售回款", "华辰科技有限公司", "5800000", "重点客户月度回款"),
                plan(today, CashPlanType.OUTFLOW, "采购付款", "华辰控股集团", "2100000", "核心原料付款"),
                plan(today.plusDays(1), CashPlanType.OUTFLOW, "薪酬社保", "华辰科技有限公司", "4680000", "月度薪酬批量支付"),
                plan(today.plusDays(2), CashPlanType.INFLOW, "项目回款", "华辰建设有限公司", "8600000", "二期工程节点款"),
                plan(today.plusDays(3), CashPlanType.OUTFLOW, "税费缴纳", "华辰控股集团", "1860000", "增值税及附加"),
                plan(today.plusDays(4), CashPlanType.INFLOW, "销售回款", "华辰科技有限公司", "3200000", "经销商回款"),
                plan(today.plusDays(5), CashPlanType.OUTFLOW, "融资还本付息", "华辰控股集团", "5200000", "流动资金贷款还本付息"),
                plan(today.plusDays(6), CashPlanType.OUTFLOW, "日常运营", "华辰国际有限公司", "680000", "境外运营费用")
        ));

        auditLogRepository.saveAll(List.of(
                new AuditLog("system", "SYNC", "BANK_ACCOUNT", basic.getId().toString(), "银企直联余额同步成功", "system"),
                new AuditLog("approver", "APPROVE", "PAYMENT", approved.getId().toString(), "审批通过付款单 " + approved.getPaymentNo(), "10.20.3.17"),
                new AuditLog("operator", "SUBMIT", "PAYMENT", pending.getId().toString(), "提交付款单 " + pending.getPaymentNo() + " 审批", "10.20.3.26"),
                new AuditLog("system", "BANK_EXECUTE", "PAYMENT", paid.getId().toString(), "银行支付成功并取得电子回单", "system")
        ));
    }

    private CashPlan plan(LocalDate date, CashPlanType type, String category, String organization,
                          String amount, String description) {
        return new CashPlan(date, type, category, money(amount), organization, description, "operator");
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }

    private void ensurePaymentPlatformAccounts() {
        if (!accountRepository.existsByAccountNo("2088123456789001")) {
            accountRepository.save(new BankAccount(
                    AccountChannel.ALIPAY,
                    "华辰控股集团", "支付宝", "ALIPAY", "支付宝企业商户账户", "2088123456789001",
                    "CNY", money("860000"), money("860000"), money("100000"),
                    AccountType.PAYMENT_PLATFORM, AccountStatus.ACTIVE
            ));
        }
        if (!accountRepository.existsByAccountNo("1900000109")) {
            accountRepository.save(new BankAccount(
                    AccountChannel.WECHAT,
                    "华辰控股集团", "微信支付", "WECHAT", "微信支付商户账户", "1900000109",
                    "CNY", money("560000"), money("560000"), money("80000"),
                    AccountType.PAYMENT_PLATFORM, AccountStatus.ACTIVE
            ));
        }
    }
}

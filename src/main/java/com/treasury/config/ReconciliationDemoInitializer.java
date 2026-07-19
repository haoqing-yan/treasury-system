package com.treasury.config;

import com.treasury.domain.BankAccount;
import com.treasury.domain.BankTransaction;
import com.treasury.domain.PaymentOrder;
import com.treasury.domain.PaymentStatus;
import com.treasury.domain.TransactionDirection;
import com.treasury.repository.AuditLogRepository;
import com.treasury.repository.BankAccountRepository;
import com.treasury.repository.BankTransactionRepository;
import com.treasury.repository.PaymentOrderRepository;
import com.treasury.domain.AuditLog;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
public class ReconciliationDemoInitializer implements ApplicationRunner {

    private final BankTransactionRepository transactionRepository;
    private final BankAccountRepository accountRepository;
    private final PaymentOrderRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;

    public ReconciliationDemoInitializer(BankTransactionRepository transactionRepository,
                                         BankAccountRepository accountRepository,
                                         PaymentOrderRepository paymentRepository,
                                         AuditLogRepository auditLogRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (transactionRepository.count() > 0 || accountRepository.count() == 0) {
            return;
        }
        List<BankAccount> accounts = accountRepository.findAll();
        BankAccount basic = account(accounts, "集团基本账户");
        BankAccount payroll = account(accounts, "员工薪酬专户");
        PaymentOrder paid = paymentRepository.findAll().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                .findFirst().orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (paid != null) {
            BankTransaction matched = new BankTransaction(
                    "BANK-" + paid.getPaymentNo(), paid.getPayerAccount(), paid.getPaidAt(),
                    TransactionDirection.OUTFLOW, paid.getPayeeName(), paid.getPayeeAccountNo(),
                    paid.getAmount(), paid.getCurrency(), paid.getPayerAccount().getBalance(), paid.getPurpose()
            );
            matched.match(paid, "BANK_REFERENCE", "银行指令号与付款单号一致");
            transactionRepository.save(matched);
        }

        transactionRepository.save(new BankTransaction(
                "TXN" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "10021",
                basic, now.minusMinutes(48), TransactionDirection.INFLOW, "远航汽车制造有限公司",
                "6222020400019827665", money("5800000"), "CNY", basic.getBalance(), "重点客户销售回款"
        ));
        BankTransaction exception = new BankTransaction(
                "TXN" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "10036",
                payroll, now.minusHours(2), TransactionDirection.OUTFLOW, "招商银行",
                "7559000000000000001", money("180"), "CNY", payroll.getBalance(), "账户管理服务费"
        );
        exception.markException("银行手续费暂未取得对应业务单据");
        transactionRepository.save(exception);

        auditLogRepository.save(new AuditLog(
                "system", "BANK_SYNC", "BANK_TRANSACTION", null,
                "银行流水同步完成，共导入 3 笔流水", "system"
        ));
    }

    private BankAccount account(List<BankAccount> accounts, String name) {
        return accounts.stream().filter(account -> account.getAccountName().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("演示账户不存在：" + name));
    }

    private BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }
}

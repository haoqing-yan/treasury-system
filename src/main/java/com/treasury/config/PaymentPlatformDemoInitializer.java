package com.treasury.config;

import com.treasury.domain.AccountChannel;
import com.treasury.domain.AuditLog;
import com.treasury.domain.BankAccount;
import com.treasury.domain.BankTransaction;
import com.treasury.domain.TransactionDirection;
import com.treasury.repository.AuditLogRepository;
import com.treasury.repository.BankAccountRepository;
import com.treasury.repository.BankTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(3)
public class PaymentPlatformDemoInitializer implements ApplicationRunner {

    private final BankAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;

    public PaymentPlatformDemoInitializer(BankAccountRepository accountRepository,
                                          BankTransactionRepository transactionRepository,
                                          AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int imported = 0;
        imported += ensureReceipt(AccountChannel.ALIPAY, "ALIPAY-DEMO-10001", "线上商城支付宝收款", "36800");
        imported += ensureReceipt(AccountChannel.WECHAT, "WECHAT-DEMO-10001", "小程序微信支付收款", "28600");
        if (imported > 0) {
            auditLogRepository.save(new AuditLog(
                    "system", "PLATFORM_SYNC", "PAYMENT_TRANSACTION", null,
                    "支付平台流水同步完成，共导入 " + imported + " 笔流水", "system"
            ));
        }
    }

    private int ensureReceipt(AccountChannel channel, String transactionNo, String purpose, String amount) {
        if (transactionRepository.existsByTransactionNo(transactionNo)) {
            return 0;
        }
        BankAccount account = accountRepository.findAll().stream()
                .filter(item -> item.getChannel() == channel)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("缺少演示支付平台账户：" + channel));
        transactionRepository.save(new BankTransaction(
                transactionNo, account, LocalDateTime.now().minusMinutes(channel == AccountChannel.ALIPAY ? 28 : 16),
                TransactionDirection.INFLOW, "消费者聚合收款", null,
                new BigDecimal(amount).setScale(2), "CNY", account.getBalance(), purpose
        ));
        return 1;
    }
}

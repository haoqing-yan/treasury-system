package com.treasury.config;

import com.treasury.domain.BankAccount;
import com.treasury.domain.BankTransaction;
import com.treasury.domain.ExceptionCategory;
import com.treasury.domain.ExceptionCaseType;
import com.treasury.domain.ExceptionSeverity;
import com.treasury.domain.PaymentOrder;
import com.treasury.domain.ReconciliationStatus;
import com.treasury.repository.BankAccountRepository;
import com.treasury.repository.BankTransactionRepository;
import com.treasury.repository.PaymentOrderRepository;
import com.treasury.service.ExceptionCaseService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(3)
public class ExceptionCaseInitializer implements ApplicationRunner {

    private final ExceptionCaseService service;
    private final PaymentOrderRepository paymentRepository;
    private final BankTransactionRepository transactionRepository;
    private final BankAccountRepository accountRepository;

    public ExceptionCaseInitializer(ExceptionCaseService service, PaymentOrderRepository paymentRepository,
                                    BankTransactionRepository transactionRepository,
                                    BankAccountRepository accountRepository) {
        this.service = service;
        this.paymentRepository = paymentRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        paymentRepository.findAll().stream().filter(PaymentOrder::isRiskFlag).forEach(payment ->
                service.register(
                        ExceptionCategory.BUSINESS,
                        ExceptionCaseType.PAYMENT_RISK, ExceptionSeverity.HIGH,
                        "付款重复风险待复核", payment.getRiskMessage(),
                        "PAYMENT", payment.getId().toString(), payment.getPaymentNo()
                ));

        transactionRepository.findAll().stream()
                .filter(transaction -> transaction.getReconciliationStatus() == ReconciliationStatus.EXCEPTION)
                .forEach(transaction -> registerTransaction(transaction));

        accountRepository.findAll().stream().filter(account ->
                        account.getAvailableBalance().compareTo(account.getLowBalanceThreshold()) < 0)
                .forEach(this::registerLowBalance);

        service.register(
                ExceptionCategory.SYSTEM,
                ExceptionCaseType.SYSTEM_INTEGRATION, ExceptionSeverity.LOW,
                "生产银行接口尚未配置",
                "当前使用模拟银行执行器，正式环境需配置银企直联适配器与回单通道",
                "SYSTEM_INTEGRATION", "BANK_ADAPTER", "银企直联"
        );
        service.register(
                ExceptionCategory.SYSTEM,
                ExceptionCaseType.SYSTEM_INTEGRATION, ExceptionSeverity.LOW,
                "支付宝开放平台接口尚未配置",
                "当前使用模拟支付宝执行器，正式环境需配置应用 ID、商户私钥和支付宝公钥",
                "SYSTEM_INTEGRATION", "ALIPAY_ADAPTER", "支付宝开放平台"
        );
        service.register(
                ExceptionCategory.SYSTEM,
                ExceptionCaseType.SYSTEM_INTEGRATION, ExceptionSeverity.LOW,
                "微信支付商户接口尚未配置",
                "当前使用模拟微信支付执行器，正式环境需配置商户号、API v3 密钥和商户证书",
                "SYSTEM_INTEGRATION", "WECHAT_ADAPTER", "微信支付商户平台"
        );
    }

    private void registerTransaction(BankTransaction transaction) {
        service.register(
                ExceptionCategory.BUSINESS,
                ExceptionCaseType.RECONCILIATION, ExceptionSeverity.MEDIUM,
                "银行流水异常待核查", transaction.getMatchMessage(),
                "BANK_TRANSACTION", transaction.getId().toString(), transaction.getTransactionNo()
        );
    }

    private void registerLowBalance(BankAccount account) {
        service.register(
                ExceptionCategory.BUSINESS,
                ExceptionCaseType.ACCOUNT_BALANCE, ExceptionSeverity.HIGH,
                "账户可用余额低于预警线",
                account.getAccountName() + " 可用余额已低于预警阈值，请核实后续资金安排",
                "BANK_ACCOUNT", account.getId().toString(), account.getAccountName()
        );
    }
}

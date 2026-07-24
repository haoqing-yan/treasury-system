package com.treasury.service;

import com.treasury.domain.AccountStatus;
import com.treasury.domain.BankAccount;
import com.treasury.domain.PaymentBatchItem;
import com.treasury.domain.PaymentOrder;
import com.treasury.repository.PaymentBatchItemRepository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PaymentBatchItemExecutor {

    private final PaymentBatchItemRepository itemRepository;
    private final ReconciliationService reconciliationService;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;
    private final ConcurrentHashMap<Long, ReentrantLock> accountLocks = new ConcurrentHashMap<>();

    public PaymentBatchItemExecutor(PaymentBatchItemRepository itemRepository,
                                    ReconciliationService reconciliationService,
                                    AuditService auditService,
                                    PlatformTransactionManager transactionManager) {
        this.itemRepository = itemRepository;
        this.reconciliationService = reconciliationService;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void execute(Long itemId, String username) {
        Long accountId = itemRepository.findWithPaymentById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("付款批次明细不存在"))
                .getPayment().getPayerAccount().getId();
        ReentrantLock lock = accountLocks.computeIfAbsent(accountId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            transactionTemplate.executeWithoutResult(status -> executeInTransaction(itemId, username));
        } finally {
            lock.unlock();
        }
    }

    private void executeInTransaction(Long itemId, String username) {
        PaymentBatchItem item = itemRepository.findWithPaymentById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("付款批次明细不存在"));
        PaymentOrder payment = item.getPayment();
        BankAccount payer = payment.getPayerAccount();
        if (payer.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("付款账户状态异常");
        }

        item.start();
        payment.beginBatchExecution();
        payer.debit(payment.getAmount());
        payment.markPaid();
        reconciliationService.recordPaidPayment(payment);
        item.succeed();
        auditService.record(username, "BATCH_EXECUTE", "PAYMENT", payment.getId().toString(),
                "批次付款执行成功：" + payment.getPaymentNo() + "，请求号 " + item.getRequestId());
    }
}

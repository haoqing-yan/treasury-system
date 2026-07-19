package com.treasury.service;

import com.treasury.domain.BankAccount;
import com.treasury.domain.BankTransaction;
import com.treasury.domain.ExceptionCategory;
import com.treasury.domain.ExceptionCaseType;
import com.treasury.domain.ExceptionSeverity;
import com.treasury.domain.PaymentOrder;
import com.treasury.domain.PaymentStatus;
import com.treasury.domain.ReconciliationStatus;
import com.treasury.domain.TransactionDirection;
import com.treasury.dto.ReconciliationDtos;
import com.treasury.repository.BankTransactionRepository;
import com.treasury.repository.PaymentOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReconciliationService {

    private final BankTransactionRepository transactionRepository;
    private final PaymentOrderRepository paymentRepository;
    private final AuditService auditService;
    private final ExceptionCaseService exceptionCaseService;

    public ReconciliationService(BankTransactionRepository transactionRepository,
                                 PaymentOrderRepository paymentRepository,
                                 AuditService auditService,
                                 ExceptionCaseService exceptionCaseService) {
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
        this.exceptionCaseService = exceptionCaseService;
    }

    @Transactional(readOnly = true)
    public List<ReconciliationDtos.Response> list(String keyword, String status) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return transactionRepository.findAllByOrderByTransactionTimeDesc().stream()
                .filter(transaction -> normalized.isBlank()
                        || transaction.getTransactionNo().toLowerCase(Locale.ROOT).contains(normalized)
                        || transaction.getCounterpartyName().toLowerCase(Locale.ROOT).contains(normalized)
                        || transaction.getPurpose().toLowerCase(Locale.ROOT).contains(normalized))
                .filter(transaction -> status == null || status.isBlank()
                        || transaction.getReconciliationStatus().name().equals(status))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationDtos.Summary summary() {
        return buildSummary(transactionRepository.findAll());
    }

    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    @Transactional
    public ReconciliationDtos.AutoMatchResult autoMatch(String username) {
        List<BankTransaction> transactions = transactionRepository.findAllByOrderByTransactionTimeDesc();
        List<PaymentOrder> payments = paymentRepository.findAll();
        int matched = 0;

        for (BankTransaction transaction : transactions) {
            if (transaction.getReconciliationStatus() != ReconciliationStatus.UNMATCHED
                    || transaction.getDirection() != TransactionDirection.OUTFLOW) {
                continue;
            }
            PaymentOrder candidate = payments.stream()
                    .filter(payment -> payment.getStatus() == PaymentStatus.PAID)
                    .filter(payment -> !transactionRepository.existsByMatchedPaymentId(payment.getId()))
                    .filter(payment -> samePayment(transaction, payment))
                    .findFirst()
                    .orElse(null);
            if (candidate != null) {
                transaction.match(candidate, "AUTO_RULE", "账户、币种、金额和收款方一致");
                matched++;
            }
        }

        auditService.record(username, "AUTO_MATCH", "RECONCILIATION", null,
                "自动对账完成，成功匹配 " + matched + " 笔银行流水");
        return new ReconciliationDtos.AutoMatchResult(matched, buildSummary(transactions));
    }

    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    @Transactional
    public ReconciliationDtos.Response manualMatch(Long transactionId, Long paymentId, String username) {
        BankTransaction transaction = get(transactionId);
        PaymentOrder payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("付款单不存在"));
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("只能匹配已支付付款单");
        }
        if (transaction.getDirection() != TransactionDirection.OUTFLOW) {
            throw new IllegalStateException("资金流入不能匹配付款单");
        }
        if (transactionRepository.existsByMatchedPaymentId(paymentId)) {
            throw new IllegalStateException("该付款单已匹配其他银行流水");
        }
        if (transaction.getBankAccount().getId().equals(payment.getPayerAccount().getId())
                && transaction.getAmount().compareTo(payment.getAmount()) == 0
                && transaction.getCurrency().equals(payment.getCurrency())) {
            transaction.match(payment, "MANUAL", "人工复核匹配");
        } else {
            throw new IllegalStateException("流水与付款单的账户、币种或金额不一致");
        }
        auditService.record(username, "MANUAL_MATCH", "BANK_TRANSACTION", transactionId.toString(),
                "手工匹配付款单 " + payment.getPaymentNo());
        exceptionCaseService.resolveBySource(
                "BANK_TRANSACTION", transactionId.toString(), username, "异常流水已完成人工匹配"
        );
        return toResponse(transaction);
    }

    @PreAuthorize("hasAnyRole('APPROVER','ADMIN')")
    @Transactional
    public ReconciliationDtos.Response markException(Long transactionId, String reason, String username) {
        BankTransaction transaction = get(transactionId);
        transaction.markException(reason);
        exceptionCaseService.register(
                ExceptionCategory.BUSINESS,
                ExceptionCaseType.RECONCILIATION, ExceptionSeverity.MEDIUM,
                "银行流水异常待核查", reason,
                "BANK_TRANSACTION", transactionId.toString(), transaction.getTransactionNo()
        );
        auditService.record(username, "MARK_EXCEPTION", "BANK_TRANSACTION", transactionId.toString(),
                "标记异常流水：" + reason);
        return toResponse(transaction);
    }

    @Transactional
    public BankTransaction recordPaidPayment(PaymentOrder payment) {
        if (transactionRepository.existsByMatchedPaymentId(payment.getId())) {
            throw new IllegalStateException("该付款单已生成银行流水");
        }
        BankAccount account = payment.getPayerAccount();
        String referencePrefix = switch (account.getChannel()) {
            case ALIPAY -> "ALIPAY-";
            case WECHAT -> "WECHAT-";
            case BANK -> "BANK-";
        };
        String matchMethod = switch (account.getChannel()) {
            case ALIPAY -> "ALIPAY_TRADE_NO";
            case WECHAT -> "WECHAT_TXN_ID";
            case BANK -> "BANK_REFERENCE";
        };
        BankTransaction transaction = new BankTransaction(
                referencePrefix + payment.getPaymentNo(), account, payment.getPaidAt(), TransactionDirection.OUTFLOW,
                payment.getPayeeName(), payment.getPayeeAccountNo(), payment.getAmount(), payment.getCurrency(),
                account.getBalance(), payment.getPurpose()
        );
        transaction.match(payment, matchMethod, "渠道交易号与付款单号一致");
        return transactionRepository.save(transaction);
    }

    private boolean samePayment(BankTransaction transaction, PaymentOrder payment) {
        return transaction.getBankAccount().getId().equals(payment.getPayerAccount().getId())
                && transaction.getAmount().compareTo(payment.getAmount()) == 0
                && transaction.getCurrency().equals(payment.getCurrency())
                && transaction.getCounterpartyName().trim().equalsIgnoreCase(payment.getPayeeName().trim());
    }

    private BankTransaction get(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("银行流水不存在"));
    }

    private ReconciliationDtos.Summary buildSummary(List<BankTransaction> transactions) {
        BigDecimal matchedAmount = transactions.stream()
                .filter(transaction -> transaction.getReconciliationStatus() == ReconciliationStatus.MATCHED)
                .map(BankTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unmatchedAmount = transactions.stream()
                .filter(transaction -> transaction.getReconciliationStatus() == ReconciliationStatus.UNMATCHED)
                .map(BankTransaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReconciliationDtos.Summary(
                transactions.size(),
                transactions.stream().filter(item -> item.getReconciliationStatus() == ReconciliationStatus.MATCHED).count(),
                transactions.stream().filter(item -> item.getReconciliationStatus() == ReconciliationStatus.UNMATCHED).count(),
                transactions.stream().filter(item -> item.getReconciliationStatus() == ReconciliationStatus.EXCEPTION).count(),
                matchedAmount, unmatchedAmount
        );
    }

    public ReconciliationDtos.Response toResponse(BankTransaction transaction) {
        PaymentOrder payment = transaction.getMatchedPayment();
        return new ReconciliationDtos.Response(
                transaction.getId(), transaction.getTransactionNo(), transaction.getBankAccount().getId(),
                transaction.getBankAccount().getChannel(),
                transaction.getBankAccount().getAccountName(),
                MaskingUtils.accountNo(transaction.getBankAccount().getAccountNo()), transaction.getTransactionTime(),
                transaction.getDirection(), transaction.getCounterpartyName(),
                MaskingUtils.accountNo(transaction.getCounterpartyAccountNo()), transaction.getAmount(),
                transaction.getCurrency(), transaction.getBalanceAfter(), transaction.getPurpose(),
                transaction.getReconciliationStatus(), payment == null ? null : payment.getId(),
                payment == null ? null : payment.getPaymentNo(), transaction.getMatchMethod(),
                transaction.getMatchMessage(), transaction.getMatchedAt()
        );
    }
}

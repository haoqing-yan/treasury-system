package com.treasury.service;

import com.treasury.domain.AccountStatus;
import com.treasury.domain.BankAccount;
import com.treasury.domain.ExceptionCategory;
import com.treasury.domain.ExceptionCaseType;
import com.treasury.domain.ExceptionSeverity;
import com.treasury.domain.PaymentOrder;
import com.treasury.domain.PaymentStatus;
import com.treasury.dto.PaymentDtos;
import com.treasury.repository.BankAccountRepository;
import com.treasury.repository.PaymentOrderRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private static final DateTimeFormatter NUMBER_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PaymentOrderRepository repository;
    private final BankAccountRepository accountRepository;
    private final AuditService auditService;
    private final ReconciliationService reconciliationService;
    private final ExceptionCaseService exceptionCaseService;

    public PaymentService(PaymentOrderRepository repository, BankAccountRepository accountRepository,
                          AuditService auditService, ReconciliationService reconciliationService,
                          ExceptionCaseService exceptionCaseService) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
        this.reconciliationService = reconciliationService;
        this.exceptionCaseService = exceptionCaseService;
    }

    @Transactional(readOnly = true)
    public List<PaymentDtos.Response> list(String keyword, String status) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream()
                .filter(payment -> normalized.isBlank()
                        || payment.getPaymentNo().toLowerCase(Locale.ROOT).contains(normalized)
                        || payment.getPayeeName().toLowerCase(Locale.ROOT).contains(normalized)
                        || payment.getPurpose().toLowerCase(Locale.ROOT).contains(normalized))
                .filter(payment -> status == null || status.isBlank() || payment.getStatus().name().equals(status))
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('payment:create')")
    @Transactional
    public PaymentDtos.Response create(PaymentDtos.CreateRequest request, String username) {
        BankAccount payer = accountRepository.findById(request.payerAccountId())
                .orElseThrow(() -> new IllegalArgumentException("付款账户不存在"));
        if (payer.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("付款账户当前不可用于支付");
        }
        if (!payer.getCurrency().equals(request.currency())) {
            throw new IllegalArgumentException("付款币种必须与付款账户币种一致");
        }

        boolean duplicate = repository.existsPotentialDuplicate(
                payer.getId(), request.payeeAccountNo(), request.amount(), LocalDateTime.now().minusHours(24)
        );
        String riskMessage = duplicate ? "24小时内存在相同收款账号和金额的付款" : null;
        PaymentOrder payment = new PaymentOrder(
                nextPaymentNo(), payer, request.payeeName(), request.payeeBankName(), request.payeeAccountNo(),
                request.amount(), request.currency(), request.purpose(), username, duplicate, riskMessage
        );
        repository.save(payment);
        if (duplicate) {
            exceptionCaseService.register(
                    ExceptionCategory.BUSINESS,
                    ExceptionCaseType.PAYMENT_RISK, ExceptionSeverity.HIGH,
                    "付款重复风险待复核", riskMessage,
                    "PAYMENT", payment.getId().toString(), payment.getPaymentNo()
            );
        }
        auditService.record(username, "CREATE", "PAYMENT", payment.getId().toString(),
                "新建付款单 " + payment.getPaymentNo() + "，金额 " + payment.getAmount());
        return toResponse(payment);
    }

    @PreAuthorize("hasAuthority('payment:submit')")
    @Transactional
    public PaymentDtos.Response submit(Long id, String username) {
        PaymentOrder payment = get(id);
        if (!payment.getApplicant().equals(username) && !isAdmin()) {
            throw new IllegalStateException("只能提交本人创建的付款单");
        }
        payment.submit();
        auditService.record(username, "SUBMIT", "PAYMENT", id.toString(),
                "提交付款单 " + payment.getPaymentNo() + " 审批");
        return toResponse(payment);
    }

    @PreAuthorize("hasAuthority('payment:approve')")
    @Transactional
    public PaymentDtos.Response approve(Long id, String username) {
        PaymentOrder payment = get(id);
        if (payment.getApplicant().equals(username) && !isAdmin()) {
            throw new IllegalStateException("申请人与审批人不能为同一人");
        }
        payment.approve(username);
        auditService.record(username, "APPROVE", "PAYMENT", id.toString(),
                "审批通过付款单 " + payment.getPaymentNo());
        return toResponse(payment);
    }

    @PreAuthorize("hasAuthority('payment:approve')")
    @Transactional
    public PaymentDtos.Response reject(Long id, String reason, String username) {
        PaymentOrder payment = get(id);
        if (payment.getApplicant().equals(username) && !isAdmin()) {
            throw new IllegalStateException("申请人与审批人不能为同一人");
        }
        payment.reject(username, reason);
        auditService.record(username, "REJECT", "PAYMENT", id.toString(),
                "驳回付款单 " + payment.getPaymentNo() + "：" + reason);
        return toResponse(payment);
    }

    @PreAuthorize("hasAuthority('payment:execute')")
    @Transactional
    public PaymentDtos.Response execute(Long id, String username) {
        PaymentOrder payment = get(id);
        BankAccount payer = payment.getPayerAccount();
        if (payer.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("付款账户状态异常，无法发送银行");
        }
        payer.debit(payment.getAmount());
        payment.markPaid();
        reconciliationService.recordPaidPayment(payment);
        auditService.record(username, "BANK_EXECUTE", "PAYMENT", id.toString(),
                "模拟" + channelName(payer.getChannel()) + "支付成功并完成流水匹配："
                        + payment.getPaymentNo());
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentDtos.Response> recent() {
        return repository.findTop8ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaymentOrder get(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("付款单不存在"));
    }

    public PaymentDtos.Response toResponse(PaymentOrder payment) {
        BankAccount payer = payment.getPayerAccount();
        return new PaymentDtos.Response(
                payment.getId(), payment.getPaymentNo(), payer.getId(), payer.getChannel(), payer.getAccountName(),
                MaskingUtils.accountNo(payer.getAccountNo()), payer.getOrganizationName(),
                payment.getPayeeName(), payment.getPayeeBankName(),
                MaskingUtils.accountNo(payment.getPayeeAccountNo()), payment.getAmount(), payment.getCurrency(),
                payment.getPurpose(), payment.getStatus(), payment.getApplicant(), payment.getApprover(),
                payment.getRejectReason(), payment.isRiskFlag(), payment.getRiskMessage(), payment.getCreatedAt(),
                payment.getSubmittedAt(), payment.getApprovedAt(), payment.getPaidAt()
        );
    }

    private String nextPaymentNo() {
        return "FK" + LocalDateTime.now().format(NUMBER_TIME) + String.format("%04d", RANDOM.nextInt(10_000));
    }

    private String channelName(com.treasury.domain.AccountChannel channel) {
        return switch (channel) {
            case ALIPAY -> "支付宝";
            case WECHAT -> "微信支付";
            case BANK -> "银行";
        };
    }

    private boolean isAdmin() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}

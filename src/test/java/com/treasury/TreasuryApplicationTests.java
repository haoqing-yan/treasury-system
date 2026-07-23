package com.treasury;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treasury.domain.AccountStatus;
import com.treasury.domain.AccountChannel;
import com.treasury.domain.BankAccount;
import com.treasury.domain.ExceptionStatus;
import com.treasury.repository.BankAccountRepository;
import com.treasury.repository.BankTransactionRepository;
import com.treasury.repository.AppUserRepository;
import com.treasury.repository.ExceptionCaseRepository;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TreasuryApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private BankTransactionRepository transactionRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private ExceptionCaseRepository exceptionCaseRepository;

    @Test
    void dashboardProvidesTreasuryOverview() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(user("admin").roles("ADMIN", "OPERATOR", "APPROVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.accountCount").value(7))
                .andExpect(jsonPath("$.cashFlow", hasSize(7)))
                .andExpect(jsonPath("$.recentPayments").isArray());
    }

    @Test
    void reconciliationSummaryContainsMatchedAndUnmatchedTransactions() throws Exception {
        mockMvc.perform(get("/api/reconciliations/summary")
                        .with(user("admin").roles("ADMIN", "OPERATOR", "APPROVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(5))
                .andExpect(jsonPath("$.matchedCount").value(1))
                .andExpect(jsonPath("$.unmatchedCount").value(3))
                .andExpect(jsonPath("$.exceptionCount").value(1));
    }

    @Test
    void alipayAndWechatChannelsAreAvailable() throws Exception {
        mockMvc.perform(get("/api/accounts")
                        .param("channel", "ALIPAY")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].channel").value("ALIPAY"))
                .andExpect(jsonPath("$[0].accountType").value("PAYMENT_PLATFORM"));

        mockMvc.perform(get("/api/accounts")
                        .param("channel", "WECHAT")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].channel").value("WECHAT"));
    }

    @Test
    void exceptionCaseCanBeClaimedAndResolved() throws Exception {
        mockMvc.perform(get("/api/exceptions/summary")
                        .with(user("approver").roles("APPROVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(6))
                .andExpect(jsonPath("$.businessCount").value(3))
                .andExpect(jsonPath("$.systemCount").value(3))
                .andExpect(jsonPath("$.openCount").value(6))
                .andExpect(jsonPath("$.highPriorityCount").value(2));

        mockMvc.perform(get("/api/exceptions")
                        .param("category", "SYSTEM")
                        .with(user("approver").roles("APPROVER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].category").value("SYSTEM"))
                .andExpect(jsonPath("$[0].type").value("SYSTEM_INTEGRATION"));

        long caseId = exceptionCaseRepository.findAll().stream()
                .filter(item -> item.getStatus() == ExceptionStatus.OPEN)
                .findFirst().orElseThrow().getId();

        mockMvc.perform(post("/api/exceptions/{id}/claim", caseId)
                        .with(user("approver").roles("APPROVER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.assignee").value("approver"));

        mockMvc.perform(post("/api/exceptions/{id}/resolve", caseId)
                        .with(user("approver").roles("APPROVER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"已核实业务凭证并完成异常处置\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolution").value("已核实业务凭证并完成异常处置"));
    }

    @Test
    void spaCanLoginThroughJsonEndpoint() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles").isArray())
                .andExpect(jsonPath("$.permissions").isArray())
                .andExpect(jsonPath("$.permissions", org.hamcrest.Matchers.hasItem("payment:execute")))
                .andExpect(jsonPath("$.permissions", org.hamcrest.Matchers.hasItem("audit:read")));

        org.assertj.core.api.Assertions.assertThat(userRepository.count()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(userRepository.findByUsernameIgnoreCase("admin").orElseThrow()
                .getPasswordHash()).startsWith("$2").doesNotContain("admin123");
    }

    @Test
    void paymentFollowsMakerCheckerAndExecutionWorkflow() throws Exception {
        BankAccount account = accountRepository.findAll().stream()
                .filter(item -> item.getStatus() == AccountStatus.ACTIVE && item.getCurrency().equals("CNY"))
                .findFirst().orElseThrow();
        BigDecimal balanceBefore = account.getAvailableBalance();

        String createBody = """
                {
                  "payerAccountId": %d,
                  "payeeName": "测试供应商有限公司",
                  "payeeBankName": "中国银行",
                  "payeeAccountNo": "6222029988776655443",
                  "amount": 125600.00,
                  "currency": "CNY",
                  "purpose": "集成测试付款"
                }
                """.formatted(account.getId());

        String createdJson = mockMvc.perform(post("/api/payments")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(createdJson);
        long paymentId = created.get("id").asLong();

        mockMvc.perform(post("/api/payments/{id}/submit", paymentId)
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/payments/{id}/approve", paymentId)
                        .with(user("approver").roles("APPROVER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(post("/api/payments/{id}/execute", paymentId)
                        .with(user("admin").roles("ADMIN", "OPERATOR", "APPROVER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        BankAccount updated = accountRepository.findById(account.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getAvailableBalance())
                .isEqualByComparingTo(balanceBefore.subtract(new BigDecimal("125600.00")));
        org.assertj.core.api.Assertions.assertThat(transactionRepository.existsByMatchedPaymentId(paymentId)).isTrue();
    }

    @Test
    void alipayPaymentCreatesChannelTransaction() throws Exception {
        BankAccount account = accountRepository.findAll().stream()
                .filter(item -> item.getChannel() == AccountChannel.ALIPAY)
                .findFirst().orElseThrow();
        String createdJson = mockMvc.perform(post("/api/payments")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payerAccountId":%d,"payeeName":"测试商户",
                                 "payeeBankName":"支付宝","payeeAccountNo":"merchant_90001",
                                 "amount":168.00,"currency":"CNY","purpose":"支付宝渠道集成测试"}
                                """.formatted(account.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payerChannel").value("ALIPAY"))
                .andReturn().getResponse().getContentAsString();
        long paymentId = objectMapper.readTree(createdJson).get("id").asLong();

        mockMvc.perform(post("/api/payments/{id}/submit", paymentId)
                        .with(user("operator").roles("OPERATOR")).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/payments/{id}/approve", paymentId)
                        .with(user("approver").roles("APPROVER")).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/payments/{id}/execute", paymentId)
                        .with(user("admin").roles("ADMIN", "OPERATOR", "APPROVER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payerChannel").value("ALIPAY"));

        var transaction = transactionRepository.findAll().stream()
                .filter(item -> item.getMatchedPayment() != null && item.getMatchedPayment().getId().equals(paymentId))
                .findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(transaction.getTransactionNo()).startsWith("ALIPAY-");
        org.assertj.core.api.Assertions.assertThat(transaction.getBankAccount().getChannel())
                .isEqualTo(AccountChannel.ALIPAY);
    }

    @Test
    void operatorCannotApprovePayments() throws Exception {
        mockMvc.perform(post("/api/payments/1/approve")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidPaymentIsRejectedBeforePersistence() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payerAccountId":1,"payeeName":"测试","payeeBankName":"测试银行",
                                 "payeeAccountNo":"123","amount":0,"currency":"CNY","purpose":"测试"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请检查表单内容"));
    }
}

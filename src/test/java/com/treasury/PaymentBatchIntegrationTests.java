package com.treasury;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.treasury.domain.AccountStatus;
import com.treasury.domain.BankAccount;
import com.treasury.repository.BankAccountRepository;
import com.treasury.repository.BankTransactionRepository;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentBatchIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BankAccountRepository accountRepository;

    @Autowired
    private BankTransactionRepository transactionRepository;

    @Test
    void approvedPaymentsCanBeScheduledAndExecutedAsBatch() throws Exception {
        BankAccount account = accountRepository.findAll().stream()
                .filter(item -> item.getStatus() == AccountStatus.ACTIVE && item.getCurrency().equals("CNY"))
                .findFirst().orElseThrow();

        String createdJson = mockMvc.perform(post("/api/payments")
                        .with(user("operator").roles("OPERATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"payerAccountId":%d,"payeeName":"批量付款测试供应商",
                                 "payeeBankName":"中国银行","payeeAccountNo":"6222029988776655001",
                                 "amount":8800.00,"currency":"CNY","purpose":"批次执行集成测试"}
                                """.formatted(account.getId())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long paymentId = objectMapper.readTree(createdJson).get("id").asLong();

        mockMvc.perform(post("/api/payments/{id}/submit", paymentId)
                        .with(user("operator").roles("OPERATOR")).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/payments/{id}/approve", paymentId)
                        .with(user("approver").roles("APPROVER")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        String batchJson = mockMvc.perform(post("/api/payment-batches")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentIds":[%d],"scheduledAt":"%s"}
                                """.formatted(paymentId, LocalDateTime.now().plusMinutes(10))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].status").value("READY"))
                .andReturn().getResponse().getContentAsString();
        JsonNode batch = objectMapper.readTree(batchJson);

        mockMvc.perform(post("/api/payment-batches/{id}/execute", batch.get("id").asLong())
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.items[0].status").value("SUCCESS"));

        org.assertj.core.api.Assertions.assertThat(transactionRepository.existsByMatchedPaymentId(paymentId)).isTrue();
    }
}

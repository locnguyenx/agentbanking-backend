package com.agentbanking.ledger.component;

import com.agentbanking.common.efm.EfmEventPublisher;
import com.agentbanking.common.test.AbstractIntegrationTest;
import com.agentbanking.ledger.domain.port.in.CustomerBalanceInquiryUseCase;
import com.agentbanking.ledger.domain.port.in.ProcessWithdrawalUseCase;
import com.agentbanking.ledger.domain.port.out.*;
import com.agentbanking.ledger.domain.service.MerchantTransactionService;
import com.agentbanking.ledger.domain.service.ReconciliationService;
import com.agentbanking.ledger.domain.service.SettlementService;
import com.agentbanking.ledger.infrastructure.external.OnboardingServiceFeignClient;
import com.agentbanking.ledger.infrastructure.external.RulesServiceFeignClient;
import com.agentbanking.ledger.infrastructure.external.SwitchAdapterBalanceClient;
import com.agentbanking.ledger.infrastructure.external.SwitchAdapterFeignClient;
import com.agentbanking.ledger.infrastructure.messaging.ReversalEventPublisher;
import com.agentbanking.ledger.infrastructure.messaging.TransactionEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@org.springframework.test.context.jdbc.Sql(statements = "UPDATE agent_float SET merchant_gps_lat = 3.1390, merchant_gps_lng = 101.6869 WHERE agent_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'")
class LedgerControllerComponentTest extends AbstractIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        IdempotencyCache idempotencyCache() {
            return new InMemoryIdempotencyCache();
        }
    }

    static class InMemoryIdempotencyCache implements IdempotencyCache {
        private final Map<String, String> store = new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public void save(String key, Object response, java.time.Duration ttl) {
            store.put(key, response.toString());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> type) {
            return (T) store.get(key);
        }

        @Override
        public boolean exists(String key) {
            return store.containsKey(key);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RulesServiceFeignClient rulesServiceFeignClient;

    @MockBean
    private SwitchAdapterFeignClient switchAdapterFeignClient;

    @MockBean
    private SwitchAdapterBalanceClient switchAdapterBalanceClient;

    @MockBean
    private OnboardingServiceFeignClient onboardingServiceFeignClient;

    @MockBean
    private TransactionEventPublisher transactionEventPublisher;

    @MockBean
    private ReversalEventPublisher reversalEventPublisher;

    @MockBean
    private SwitchServicePort switchServicePort;

    @MockBean
    private AgentRepository agentRepository;

    @MockBean
    private MerchantTransactionService merchantTransactionService;

    @MockBean
    private EfmEventPublisher efmEventPublisher;

    @MockBean
    private DiscrepancyCaseRepository discrepancyCaseRepository;

    @MockBean
    private SettlementService settlementService;

    @MockBean
    private ReconciliationService reconciliationService;

    @MockBean
    private CustomerBalanceInquiryUseCase customerBalanceInquiryUseCase;

    private static final UUID AGENT_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    private void stubSwitchSuccess() {
        when(switchServicePort.debitAccount(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("responseCode", "00", "status", "SUCCESS", "switchReference", "SWITCH-123", "referenceNumber", "REF-123"));
        when(switchServicePort.creditAccount(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("responseCode", "00", "status", "SUCCESS", "switchReference", "SWITCH-456", "referenceNumber", "REF-456"));
    }

    private void stubVelocityCheck() {
        when(rulesServiceFeignClient.checkVelocity(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("passed", true));
    }

    @Test
    void getBalance_shouldReturnAgentBalance() throws Exception {
        mockMvc.perform(get("/internal/balance/{agentId}", AGENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(AGENT_ID.toString()))
                .andExpect(jsonPath("$.balance").exists())
                .andExpect(jsonPath("$.currency").value("MYR"));
    }

    @Test
    void getBalance_withNonExistentAgent_shouldReturn400() throws Exception {
        UUID nonExistentAgentId = UUID.randomUUID();
        
        mockMvc.perform(get("/internal/balance/{agentId}", nonExistentAgentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.error.code").value("ERR_SYS_AGENT_FLOAT_NOT_FOUND"));
    }

    @Test
    void getJournalEntries_shouldReturnEntries() throws Exception {
        String workflowId = UUID.randomUUID().toString();
        
        mockMvc.perform(get("/internal/journal")
                        .param("workflowId", workflowId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void postDebit_shouldProcessWithdrawal() throws Exception {
        stubSwitchSuccess();
        stubVelocityCheck();
        
        String requestBody = """
            {
                "agentId": "%s",
                "amount": 500.00,
                "customerFee": 2.50,
                "agentCommission": 10.00,
                "bankShare": 5.00,
                "idempotencyKey": "test-debit-%s",
                "customerCardMasked": "411111******1111",
                "geofenceLat": 3.1390,
                "geofenceLng": 101.6869,
                "agentTier": "GOLD",
                "targetBin": "123456"
            }
            """.formatted(AGENT_ID, UUID.randomUUID());

        mockMvc.perform(post("/internal/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.balance").exists());
    }

    @Test
    void postCredit_shouldProcessDeposit() throws Exception {
        stubSwitchSuccess();
        stubVelocityCheck();
        String requestBody = """
            {
                "agentId": "%s",
                "amount": 1000.00,
                "customerFee": 2.50,
                "agentCommission": 15.00,
                "bankShare": 5.00,
                "idempotencyKey": "test-credit-%s",
                "customerMykad": "900101015566",
                "geofenceLat": 3.1390,
                "geofenceLng": 101.6869,
                "destinationAccount": "1234567890"
            }
            """.formatted(AGENT_ID, UUID.randomUUID());

        mockMvc.perform(post("/internal/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.balance").exists());
    }

    @Test
    void postReverse_withValidTransaction_shouldReverse() throws Exception {
        mockMvc.perform(post("/internal/reverse/{transactionId}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void getBackofficeDashboard_shouldReturnDashboard() throws Exception {
        mockMvc.perform(get("/internal/backoffice/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAgents").exists())
                .andExpect(jsonPath("$.totalTransactions").exists())
                .andExpect(jsonPath("$.totalVolume").exists());
    }

    @Test
    void getBackofficeAgents_shouldReturnAgents() throws Exception {
        mockMvc.perform(get("/internal/backoffice/agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    void getBackofficeTransactions_shouldReturnTransactions() throws Exception {
        mockMvc.perform(get("/internal/backoffice/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    void getBackofficeSettlement_shouldReturnSettlement() throws Exception {
        mockMvc.perform(get("/internal/backoffice/settlement")
                        .param("date", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-03-31"))
                .andExpect(jsonPath("$.totalDebits").exists())
                .andExpect(jsonPath("$.totalCredits").exists());
    }

    @Test
    void getTransactionsHasPending_shouldCheckPending() throws Exception {
        mockMvc.perform(get("/internal/transactions/has-pending")
                        .param("agentId", AGENT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPending").isBoolean());
    }

    @Test
    void getTransactionsCountByStatus_shouldReturnCount() throws Exception {
        mockMvc.perform(get("/internal/transactions/count-by-status")
                        .param("agentId", AGENT_ID.toString())
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").isNumber());
    }

    @Test
    void getTransactionsExistsByStatus_shouldCheckExistence() throws Exception {
        mockMvc.perform(get("/internal/transactions/exists-by-status")
                        .param("agentId", AGENT_ID.toString())
                        .param("statuses", "COMPLETED", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").isBoolean());
    }

    @Test
    void postBalanceInquiry_withValidRequest_shouldReturnBalance() throws Exception {
        when(customerBalanceInquiryUseCase.inquire(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CustomerBalanceInquiryUseCase.CustomerBalanceResponse(
                        new BigDecimal("5000.00"), "MYR", "****1234"));

        String requestBody = """
            {
                "encryptedCardData": "encrypted123",
                "pinBlock": "pin123"
            }
            """;

        mockMvc.perform(post("/internal/balance-inquiry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.balance").exists());
    }

    @Test
    void postBalanceInquiry_withInvalidRequest_shouldReturnError() throws Exception {
        String requestBody = """
            {
                "encryptedCardData": "",
                "pinBlock": ""
            }
            """;

        mockMvc.perform(post("/internal/balance-inquiry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void postDebit_withInvalidAgentId_shouldReturn400() throws Exception {
        stubVelocityCheck();
        
        String requestBody = """
            {
                "agentId": "not-a-valid-uuid",
                "amount": 500.00,
                "customerFee": 2.50,
                "agentCommission": 10.00,
                "bankShare": 5.00,
                "idempotencyKey": "test-invalid-uuid-%s"
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/internal/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void postDebit_withMissingRequiredFields_shouldReturn400() throws Exception {
        stubVelocityCheck();
        
        String requestBody = """
            {
                "agentId": "%s"
            }
            """.formatted(AGENT_ID);

        mockMvc.perform(post("/internal/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void postDebit_withNegativeAmount_shouldReturn400() throws Exception {
        stubVelocityCheck();
        
        String requestBody = """
            {
                "agentId": "%s",
                "amount": -100.00,
                "customerFee": 2.50,
                "agentCommission": 10.00,
                "bankShare": 5.00,
                "idempotencyKey": "test-negative-%s",
                "customerCardMasked": "411111******1111"
            }
            """.formatted(AGENT_ID, UUID.randomUUID());

        mockMvc.perform(post("/internal/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
}

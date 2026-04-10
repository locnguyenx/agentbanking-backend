package com.agentbanking.ledger.integration;

import com.agentbanking.common.efm.EfmEventPublisher;
import com.agentbanking.common.exception.LedgerException;
import com.agentbanking.common.test.AbstractIntegrationTest;
import com.agentbanking.ledger.domain.port.in.CustomerBalanceInquiryUseCase;
import com.agentbanking.ledger.domain.port.in.ProcessWithdrawalUseCase;
import com.agentbanking.ledger.domain.port.out.*;
import com.agentbanking.ledger.domain.service.MerchantTransactionService;
import com.agentbanking.ledger.infrastructure.external.RulesServiceFeignClient;
import com.agentbanking.ledger.infrastructure.messaging.ReversalEventPublisher;
import com.agentbanking.ledger.infrastructure.messaging.TransactionEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@org.springframework.test.context.jdbc.Sql(statements = "UPDATE agent_float SET merchant_gps_lat = 3.1390, merchant_gps_lng = 101.6869 WHERE agent_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'")
class LedgerIntegrationTest extends AbstractIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        IdempotencyCache idempotencyCache() {
            return new IdempotencyCache() {
                private final Map<String, String> store = new ConcurrentHashMap<>();
                private final ObjectMapper mapper = new ObjectMapper();

                @Override
                public void save(String key, Object response, Duration ttl) {
                    try {
                        store.put(key, mapper.writeValueAsString(response));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                @SuppressWarnings("unchecked")
                public <T> T get(String key, Class<T> type) throws JsonProcessingException {
                    String json = store.get(key);
                    if (json == null) return null;
                    return mapper.readValue(json, type);
                }

                @Override
                public boolean exists(String key) {
                    return store.containsKey(key);
                }
            };
        }
    }

    @Autowired
    private ProcessWithdrawalUseCase processWithdrawalUseCase;

    @MockBean
    private RulesServiceFeignClient rulesServiceFeignClient;

    @MockBean
    private com.agentbanking.ledger.infrastructure.external.SwitchAdapterFeignClient switchAdapterFeignClient;

    @MockBean
    private com.agentbanking.ledger.infrastructure.external.SwitchAdapterBalanceClient switchAdapterBalanceClient;

    @MockBean
    private com.agentbanking.ledger.infrastructure.external.OnboardingServiceFeignClient onboardingServiceFeignClient;

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
    private com.agentbanking.ledger.domain.port.out.DiscrepancyCaseRepository discrepancyCaseRepository;

    @MockBean
    private com.agentbanking.ledger.domain.service.SettlementService settlementService;

    @MockBean
    private com.agentbanking.ledger.domain.service.ReconciliationService reconciliationService;

    @MockBean
    private CustomerBalanceInquiryUseCase customerBalanceInquiryUseCase;

    private static final UUID AGENT_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    private void stubSwitchSuccess() {
        when(switchServicePort.debitAccount(any(), any(), any()))
                .thenReturn(Map.of("success", true, "responseCode", "00", "switchReference", "REF123", "referenceNumber", "RN123"));
    }

    private void stubVelocityCheck() {
        when(rulesServiceFeignClient.checkVelocity(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("passed", true));
    }

    @Test
    void shouldProcessWithdrawalEndToEnd() {
        stubVelocityCheck();
        stubSwitchSuccess();

        Map<String, Object> result = processWithdrawalUseCase.processWithdrawal(
                AGENT_ID,
                new BigDecimal("500.00"),
                new BigDecimal("2.50"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                "integ-e2e-" + UUID.randomUUID(),
                "411111******1111",
                new BigDecimal("3.1390"),
                new BigDecimal("101.6869"),
                "BRONZE",
                "123456");

        assertThat(result.get("status")).isEqualTo("COMPLETED");
        assertThat(result.get("transactionId")).isNotNull();
    }

    @Test
    void shouldReturnCachedResponseOnIdempotency() {
        stubVelocityCheck();
        stubSwitchSuccess();

        String idempotencyKey = "integ-idem-" + UUID.randomUUID();

        Map<String, Object> result1 = processWithdrawalUseCase.processWithdrawal(
                AGENT_ID,
                new BigDecimal("300.00"),
                new BigDecimal("2.50"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                idempotencyKey,
                "411111******1111",
                new BigDecimal("3.1390"),
                new BigDecimal("101.6869"),
                "BRONZE",
                "123456");

        Map<String, Object> result2 = processWithdrawalUseCase.processWithdrawal(
                AGENT_ID,
                new BigDecimal("300.00"),
                new BigDecimal("2.50"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                idempotencyKey,
                "411111******1111",
                new BigDecimal("3.1390"),
                new BigDecimal("101.6869"),
                "BRONZE",
                "123456");

        assertThat(result1.get("status")).isEqualTo("COMPLETED");
        assertThat(result1.get("transactionId").toString()).isEqualTo(result2.get("transactionId").toString());
    }

    @Test
    @Sql(statements = "UPDATE agent_float SET merchant_gps_lat = 3.1390, merchant_gps_lng = 101.6869 WHERE agent_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'")
    void shouldRejectWithdrawalOutsideGeofence() {
        stubVelocityCheck();

        LedgerException ex = Assertions.assertThrows(LedgerException.class,
                () -> processWithdrawalUseCase.processWithdrawal(
                        AGENT_ID,
                        new BigDecimal("100.00"),
                        new BigDecimal("2.50"),
                        new BigDecimal("10.00"),
                        new BigDecimal("5.00"),
                        "integ-geo-" + UUID.randomUUID(),
                        "411111******1111",
                        new BigDecimal("3.1500"),
                        new BigDecimal("101.7000"),
                        "BRONZE",
                        "123456"));
        assertThat(ex.getErrorCode()).isEqualTo("ERR_BIZ_GEOFENCE_VIOLATION");
    }
}

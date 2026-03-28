package com.agentbanking.ledger.integration;

import com.agentbanking.common.efm.EfmEventPublisher;
import com.agentbanking.common.exception.LedgerException;
import com.agentbanking.ledger.domain.port.in.CustomerBalanceInquiryUseCase;
import com.agentbanking.ledger.domain.port.in.ProcessWithdrawalUseCase;
import com.agentbanking.ledger.domain.port.out.*;
import com.agentbanking.ledger.domain.service.LedgerService;
import com.agentbanking.ledger.domain.service.MerchantTransactionService;
import com.agentbanking.ledger.infrastructure.external.RulesServiceFeignClient;
import com.agentbanking.ledger.infrastructure.messaging.ReversalEventPublisher;
import com.agentbanking.ledger.infrastructure.messaging.TransactionEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class LedgerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("agentbanking")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
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
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @MockBean
    private CustomerBalanceInquiryUseCase customerBalanceInquiryUseCase;

    private static final UUID AGENT_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    private void stubVelocityCheck() {
        when(rulesServiceFeignClient.checkVelocity(org.mockito.ArgumentMatchers.any()))
                .thenReturn(Map.of("passed", true));
    }

    @Test
    void shouldProcessWithdrawalEndToEnd() {
        stubVelocityCheck();

        Map<String, Object> result = processWithdrawalUseCase.processWithdrawal(
                AGENT_ID,
                new BigDecimal("500.00"),
                new BigDecimal("2.50"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                "integ-e2e-" + UUID.randomUUID(),
                "411111******1111",
                new BigDecimal("3.1390"),
                new BigDecimal("101.6869"));

        assertThat(result.get("status")).isEqualTo("COMPLETED");
        assertThat(result.get("transactionId")).isNotNull();
    }

    @Test
    void shouldReturnCachedResponseOnIdempotency() {
        stubVelocityCheck();

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
                new BigDecimal("101.6869"));

        Map<String, Object> result2 = processWithdrawalUseCase.processWithdrawal(
                AGENT_ID,
                new BigDecimal("300.00"),
                new BigDecimal("2.50"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                idempotencyKey,
                "411111******1111",
                new BigDecimal("3.1390"),
                new BigDecimal("101.6869"));

        assertThat(result1.get("status")).isEqualTo("COMPLETED");
        assertThat(result2.get("status")).isEqualTo("COMPLETED");
        assertThat(result1.get("transactionId")).isEqualTo(result2.get("transactionId"));
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
                        new BigDecimal("101.7000")));
        assertThat(ex.getErrorCode()).isEqualTo("ERR_BIZ_GEOFENCE_VIOLATION");
    }
}

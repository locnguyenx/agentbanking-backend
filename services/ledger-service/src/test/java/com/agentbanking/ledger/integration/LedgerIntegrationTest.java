package com.agentbanking.ledger.integration;

import com.agentbanking.common.exception.LedgerException;
import com.agentbanking.common.test.AbstractIntegrationTest;
import com.agentbanking.ledger.domain.port.in.ProcessWithdrawalUseCase;
import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ledger Service Integration Tests.
 *
 * DEPENDENCIES:
 * - Testcontainers: PostgreSQL, Redis, Kafka (automatic via AbstractIntegrationTest)
 * - Docker Services (via static containers):
 *   - rules-service (port 8081) - for velocity check, fee calculation
 *   - switch-adapter-service (port 8083) - for balance inquiry, transaction processing
 *
 * per .agents/rules/testing-debugging.md:
 * - Internal services are tested via real Feign calls using Testcontainers
 */
@Sql(statements = "UPDATE agent_float SET merchant_gps_lat = 3.1390, merchant_gps_lng = 101.6869 WHERE agent_id = 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'")
class LedgerIntegrationTest extends AbstractIntegrationTest {

    static final Network NETWORK = Network.newNetwork();

    static GenericContainer<?> rulesService = new GenericContainer<>(
            DockerImageName.parse("agentbanking/rules-service:latest")
    )
            .withNetwork(NETWORK)
            .withNetworkAliases("rules-service")
            .withExposedPorts(8081)
            .withEnv("SERVER_PORT", "8081")
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("POSTGRES_HOST", "postgres")
            .withEnv("POSTGRES_PORT", "5432")
            .withEnv("KAFKA_HOST", "kafka")
            .withEnv("KAFKA_PORT", "9092")
            .waitingFor(Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofSeconds(120)))
            .withStartupTimeout(Duration.ofSeconds(180));

    static GenericContainer<?> switchAdapterService = new GenericContainer<>(
            DockerImageName.parse("agentbanking/switch-adapter-service:latest")
    )
            .withNetwork(NETWORK)
            .withNetworkAliases("switch-adapter-service")
            .withExposedPorts(8083)
            .withEnv("SERVER_PORT", "8083")
            .withEnv("SPRING_PROFILES_ACTIVE", "test")
            .withEnv("POSTGRES_HOST", "postgres")
            .withEnv("POSTGRES_PORT", "5432")
            .waitingFor(Wait.forHttp("/actuator/health").withStartupTimeout(Duration.ofSeconds(60)))
            .withStartupTimeout(Duration.ofSeconds(120));

    static {
        rulesService.start();
        switchAdapterService.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("rules-service.url", () -> "http://rules-service:8081");
        registry.add("switch-adapter-service.url", () -> "http://switch-adapter-service:8083");
        registry.add("switch-adapter.url", () -> "http://switch-adapter-service:8083");
    }

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

    private static final UUID AGENT_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @Test
    void shouldProcessWithdrawalEndToEnd() {
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
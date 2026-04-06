package com.agentbanking.orchestrator.integration;

import com.agentbanking.common.test.AbstractIntegrationTest;
import com.agentbanking.orchestrator.infrastructure.external.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Base class for orchestrator integration tests.
 * 
 * Extends AbstractIntegrationTest (PostgreSQL, Redis, Kafka) and adds:
 * - Temporal server container for real workflow testing
 * - Mocked Feign clients for all external services
 * 
 * The orchestrator has a unique dependency on Temporal that other services don't have,
 * so we isolate this test infrastructure here rather than polluting the shared base.
 */
public abstract class AbstractOrchestratorIntegrationTest extends AbstractIntegrationTest {

    // Temporal test container
    static final GenericContainer<?> temporal = new GenericContainer<>("temporalio/auto-setup:1.25.1")
            .withExposedPorts(7233)
            .withEnv("DB", "postgresql")
            .withEnv("DB_PORT", "5432")
            .withEnv("POSTGRES_USER", "postgres")
            .withEnv("POSTGRES_PWD", "postgres")
            .withEnv("POSTGRES_SEEDS", postgres.getHost())
            .withEnv("DYNAMIC_CONFIG_FILE_PATH", "/etc/temporal/dynamicconfig/development-sql.yaml")
            .dependsOn(postgres)
            .waitingFor(Wait.forLogMessage(".*Started workflow dispatcher.*", 1));

    static {
        temporal.start();
    }

    @DynamicPropertySource
    static void registerTemporalProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.temporal.connection.target", 
                () -> temporal.getHost() + ":" + temporal.getMappedPort(7233));
        registry.add("temporal.task-queue", () -> "test-agent-banking-tasks");
    }

    // Mock all external Feign clients - these are tested separately in unit tests
    // The integration test focuses on: controller → use case → workflow router → Temporal workflow start → status polling

    @MockBean
    protected SwitchAdapterClient switchAdapterClient;

    @MockBean
    protected LedgerServiceClient ledgerServiceClient;

    @MockBean
    protected RulesServiceClient rulesServiceClient;

    @MockBean
    protected BillerServiceClient billerServiceClient;

    @MockBean
    protected CbsServiceClient cbsServiceClient;

    @MockBean
    protected TelcoAggregatorClient telcoAggregatorClient;

    @MockBean
    protected EWalletProviderClient ewalletProviderClient;

    @MockBean
    protected ESSPServiceClient esspServiceClient;

    @MockBean
    protected PINInventoryClient pinInventoryClient;

    @MockBean
    protected QRPaymentClient qrPaymentClient;

    @MockBean
    protected RequestToPayClient requestToPayClient;

    @MockBean
    protected MerchantTransactionClient merchantTransactionClient;
}

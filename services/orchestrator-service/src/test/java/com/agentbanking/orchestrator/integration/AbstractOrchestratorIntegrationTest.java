package com.agentbanking.orchestrator.integration;

import com.agentbanking.common.test.AbstractIntegrationTest;
import com.agentbanking.orchestrator.infrastructure.external.*;
import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for orchestrator integration tests.
 * 
 * Extends AbstractIntegrationTest (PostgreSQL, Redis, Kafka) and adds:
 * - In-memory Temporal test server for workflow testing
 * - Mocked Feign clients for all external services
 * 
 * Uses TestWorkflowEnvironment (in-memory) instead of Docker container because:
 * - Starts in <1 second vs 60+ seconds for Docker
 * - No Docker daemon dependency
 * - Tests the same workflow lifecycle logic
 * - Standard Temporal testing pattern for integration tests
 * 
 * The integration test focuses on: controller → use case → workflow router → Temporal workflow start → status polling
 * External service calls are mocked; the Temporal workflow lifecycle is real.
 */
public abstract class AbstractOrchestratorIntegrationTest extends AbstractIntegrationTest {

    // In-memory Temporal test server
    protected static TestWorkflowEnvironment testEnv;
    protected static WorkflowClient testWorkflowClient;

    @BeforeAll
    static void setUpTemporal() {
        testEnv = TestWorkflowEnvironment.newInstance();
        testWorkflowClient = testEnv.getWorkflowClient();
    }

    @AfterAll
    static void tearDownTemporal() {
        if (testEnv != null) {
            testEnv.close();
        }
    }

    @DynamicPropertySource
    static void registerTemporalProperties(DynamicPropertyRegistry registry) {
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

    /**
     * Test configuration that overrides the autoconfigured WorkflowClient
     * with the one from the in-memory test server.
     */
    @TestConfiguration
    static class TemporalTestConfig {

        @Bean
        @Primary
        public WorkflowClient testWorkflowClient() {
            return testWorkflowClient;
        }
    }
}

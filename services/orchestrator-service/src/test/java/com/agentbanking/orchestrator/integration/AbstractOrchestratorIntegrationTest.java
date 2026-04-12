package com.agentbanking.orchestrator.integration;

import com.agentbanking.common.test.AbstractIntegrationTest;
import com.agentbanking.orchestrator.infrastructure.external.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for orchestrator integration tests.
 * 
 * Extends AbstractIntegrationTest (PostgreSQL, Redis, Kafka) and adds:
 * - Real Temporal server for workflow testing (via Docker)
 * - Mocked Feign clients for all external services
 * 
 * Uses real Temporal server in Docker instead of in-memory test environment because:
 * - Avoids TypeAlreadyRegisteredException from conflicting auto-registration
 * - Tests against production-like environment
 * - Temporal server must be running: docker compose up -d temporal temporal-postgres
 * 
 * The integration test focuses on: controller → use case → workflow router → Temporal workflow start → status polling
 * External service calls are mocked; the Temporal workflow lifecycle is real.
 */
@SpringBootTest(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver"
})
@ActiveProfiles("test")
public abstract class AbstractOrchestratorIntegrationTest extends AbstractIntegrationTest {

    // Temporal auto-configuration is excluded, so no manual setup needed

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

    // No manual test configuration needed - using auto-configured Temporal setup
}

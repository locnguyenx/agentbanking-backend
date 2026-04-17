package com.agentbanking.orchestrator.integration;

import com.agentbanking.common.test.AbstractIntegrationTest;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Base class for orchestrator integration tests.
 * 
 * ARCHITECTURE: Two Testing Modes
 * 
 * === Mode 1: True Integration Tests (RECOMMENDED) ===
 * Prerequisites: docker compose --profile all up -d
 * - Tests call REAL internal microservices (rules, ledger, switch, biller, etc.)
 * - Verifies API contracts between services
 * - Tests full business logic end-to-end
 * 
 * === Mode 2: Isolated Tests (Development Only) ===  
 * - Without docker services, tests will fail (expected)
 * - No mocks in this class - proper fallback handling via Feign
 * 
 * External mocked in tests (NOT internal):
 * - mock-server for core banking, card network (via docker-compose)
 */
@SpringBootTest(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver"
})
@ActiveProfiles("test")
public abstract class AbstractOrchestratorIntegrationTest extends AbstractIntegrationTest {

    /**
     * ONLY Mock: WorkflowFactory (Temporal test infrastructure)
     * 
     * Why this is OK:
     * - Temporal is the workflow ENGINE, not business logic
     * - We're testing orchestrator responses, not Temporal engine
     * - Real workflows would execute with docker compose
     */
    @MockBean
    protected WorkflowFactory workflowFactory;

    // NO mocks for Feign clients - they call real internal services
    // If services unavailable, Feign fallback factory handles gracefully
    
    // No manual test configuration needed - using auto-configured infrastructure
}
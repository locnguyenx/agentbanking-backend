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
 * per .agents/rules/testing-debugging.md:
 * - Testcontainers: PostgreSQL, Redis, Kafka (automatic)
 * - Docker required: Temporal only (`docker compose up -d temporal`)
 * - NOT mocked: Internal services (rules, ledger, switch, biller, etc.)
 *
 * Architecture:
 * - Extends AbstractIntegrationTest (PostgreSQL, Redis, Kafka via Testcontainers)
 * - Tests test REAL business logic via internal service calls
 * - 1 mock for WorkflowFactory (Temporal mock doesn't work properly)
 */
@SpringBootTest(properties = {
    "spring.datasource.driver-class-name=org.postgresql.Driver"
})
@ActiveProfiles("test")
public abstract class AbstractOrchestratorIntegrationTest extends AbstractIntegrationTest {

    /**
     * WHY MOCKED: Temporal mock doesn't work properly
     *
     * - Tried mocking Temporal before, didn't work properly
     * - Temporal is workflow ENGINE, not business logic
     * - We test orchestrator responses, not Temporal engine
     */
    @MockBean
    protected WorkflowFactory workflowFactory;

    // NO mocks for Feign clients - they call real internal services
}
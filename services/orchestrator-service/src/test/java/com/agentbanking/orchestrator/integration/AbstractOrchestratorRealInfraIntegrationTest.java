package com.agentbanking.orchestrator.integration;

import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Abstract base class for Orchestrator Integration Tests.
 *
 * per .agents/rules/testing-debugging.md:
 * - Testcontainers: PostgreSQL, Redis, Kafka (automatic)
 * - Docker required: Temporal only (`docker compose up -d temporal`)
 * - NOT mocked: Internal services (rules, ledger, switch, biller)
 *
 * Architecture:
 * - Tests test REAL business logic via internal service calls
 * - Feign fallback factories handle service unavailability
 * - 1 mock for WorkflowFactory (Temporal mock doesn't work properly)
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractOrchestratorRealInfraIntegrationTest {

    /**
     * WHY MOCKED: Temporal mock doesn't work properly
     *
     * - Tried mocking Temporal before, didn't work properly
     * - Temporal is workflow ENGINE, not business logic
     * - We test orchestrator → internal services → response
     * - NOT testing Temporal workflow execution
     */
    @MockBean
    protected WorkflowFactory workflowFactory;

    @BeforeEach
    void setUpMocks() {
        when(workflowFactory.startWorkflow(any(), any(String.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
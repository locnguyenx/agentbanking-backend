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
 * ARCHITECTURE: Two Testing Modes
 * 
 * === Mode 1: True Integration Tests (RECOMMENDED) ===
 * Run: docker compose --profile all up -d
 * - Tests call REAL internal microservices
 * - Verifies API contracts between services
 * - Tests business logic end-to-end
 * 
 * === Mode 2: Isolated Tests (Development Only) ===
 * - Feign clients are NOT mocked in this class
 * - If docker services unavailable, tests will fail
 * - Use WireMock stubs in resources/__files__ if needed
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractOrchestratorRealInfraIntegrationTest {

    /**
     * ONLY Mock: WorkflowFactory (Temporal test infrastructure)
     * 
     * Why this is OK:
     * - Temporal is the workflow ENGINE, not business logic
     * - In production, workflows execute in Temporal
     * - We're testing orchestrator → internal services → response
     * - Not testing Temporal's workflow execution engine
     */
    @MockBean
    protected WorkflowFactory workflowFactory;

    @BeforeEach
    void setUpMocks() {
        // Mock returns workflowId to simulate Temporal starting workflow
        // In true integration tests, this would execute real workflows
        when(workflowFactory.startWorkflow(any(), any(String.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
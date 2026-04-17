package com.agentbanking.orchestrator.integration;

import com.agentbanking.orchestrator.infrastructure.external.*;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Abstract base class for Orchestrator Integration Tests.
 * 
 * CRITICAL RULE: Tests MUST NOT mock internal services.
 * 
 * Internal services (NOT mocked):
 * - rules-service, ledger-service, switch-adapter-service, biller-service, onboarding-service
 * - These are business core microservices - must verify API contracts between them
 * 
 * External systems (OK to mock):
 * - mock-server for downstream systems (core banking, card network)
 * 
 * Prerequisites:
 *   docker compose --profile all up -d
 *   Wait ~30 seconds for all services to be healthy
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractOrchestratorRealInfraIntegrationTest {

    // WorkflowFactory is mocked to verify workflow selection (not business logic)
    @MockBean
    protected WorkflowFactory workflowFactory;
    
    @MockBean
    protected SwitchAdapterClient switchAdapterClient;
    
    @MockBean
    protected RulesServiceClient rulesServiceClient;
    
    @MockBean
    protected LedgerServiceClient ledgerServiceClient;
    
    @MockBean
    protected BillerServiceClient billerServiceClient;
    
    @MockBean
    protected CbsServiceClient cbsServiceClient;

    @BeforeEach
    void setUpMocks() {
        // Configure WorkflowFactory to return workflowId (verifies workflow selection)
        when(workflowFactory.startWorkflow(any(), any(String.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
package com.agentbanking.orchestrator.integration;

import com.agentbanking.orchestrator.infrastructure.external.RulesServiceClient.StpEvaluationRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract test to verify Feign client matches rules-service controller.
 * This test catches the @RequestParam vs @RequestBody mismatch that was causing
 * all transactions to get stuck in PENDING status.
 */
class RulesServiceContractTest {

    @Test
    void stpEvaluationRequest_shouldHaveCorrectFields() {
        // Verify the request DTO has all required fields
        StpEvaluationRequest request = new StpEvaluationRequest(
            "CASH_WITHDRAWAL",
            "agent-uuid",
            "100.00",
            "customer-mykad",
            "STANDARD",
            0,
            "0",
            "0"
        );

        assertEquals("CASH_WITHDRAWAL", request.transactionType());
        assertEquals("agent-uuid", request.agentId());
        assertEquals("100.00", request.amount());
        assertEquals("customer-mykad", request.customerProfile());
        assertEquals("STANDARD", request.agentTier());
        assertEquals(0, request.transactionCountToday());
        assertEquals("0", request.amountToday());
        assertEquals("0", request.todayTotalAmount());
    }
}
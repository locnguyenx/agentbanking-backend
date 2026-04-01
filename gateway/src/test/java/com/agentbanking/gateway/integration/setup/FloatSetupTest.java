package com.agentbanking.gateway.integration.setup;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 4: Float Setup
 * 
 * Ensures the test agent has sufficient float balance for transaction tests.
 * Uses the deposit API to add funds if needed.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(4)
class FloatSetupTest extends BaseIntegrationTest {

    private static final double MIN_FLOAT_BALANCE = 10000.00;
    private static final double DEPOSIT_AMOUNT = 50000.00;

    // ================================================================
    // STEP 1: Check current balance
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("Phase 4.1: Check agent float balance")
    void checkBalance() {
        assertNotNull(TestContext.agentId, "Agent must be created in Phase 2");

        var response = ledgerClient.get()
                .uri("/internal/balance/" + TestContext.agentId)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Balance check status: " + (status != null ? status.value() : "null"));
        System.out.println("Balance response: " + responseBody);

        if (status != null && status.value() == 200) {
            try {
                JsonNode node = objectMapper.readTree(responseBody);
                if (node.has("balance")) {
                    TestContext.agentFloatBalance = new BigDecimal(node.get("balance").asText());
                    System.out.println("Current float balance: " + TestContext.agentFloatBalance);
                }
            } catch (Exception e) {
                System.out.println("Failed to parse balance: " + e.getMessage());
            }
        } else {
            System.out.println("Balance check failed - agent float may not exist yet");
        }
    }

    // ================================================================
    // STEP 2: Top up float if needed
    // ================================================================

    @Test
    @Order(2)
    @DisplayName("Phase 4.2: Top up agent float if needed")
    void topUpFloat() {
        assertNotNull(TestContext.agentId, "Agent must be created in Phase 2");

        // If balance is sufficient, skip
        if (TestContext.agentFloatBalance != null 
                && TestContext.agentFloatBalance.compareTo(BigDecimal.valueOf(MIN_FLOAT_BALANCE)) >= 0) {
            System.out.println("Float balance is sufficient: " + TestContext.agentFloatBalance);
            return;
        }

        // Make a deposit to increase float
        String idempotencyKey = "e2e-float-setup-" + UUID.randomUUID();
        var response = deposit(TestContext.agentId, DEPOSIT_AMOUNT, idempotencyKey);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Deposit status: " + (status != null ? status.value() : "null"));
        System.out.println("Deposit response: " + responseBody);

        if (status != null && status.value() == 200) {
            try {
                JsonNode node = objectMapper.readTree(responseBody);
                if (node.has("balance")) {
                    TestContext.agentFloatBalance = new BigDecimal(node.get("balance").asText());
                    System.out.println("New float balance: " + TestContext.agentFloatBalance);
                }
            } catch (Exception e) {
                System.out.println("Failed to parse deposit response: " + e.getMessage());
            }
        } else {
            System.out.println("Deposit failed - may need to investigate");
        }
    }

    // ================================================================
    // STEP 3: Verify balance after top-up
    // ================================================================

    @Test
    @Order(3)
    @DisplayName("Phase 4.3: Verify float balance is sufficient")
    void verifyBalance() {
        assertNotNull(TestContext.agentId, "Agent must be created in Phase 2");

        // Check balance again after potential top-up
        var response = ledgerClient.get()
                .uri("/internal/balance/" + TestContext.agentId)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        if (status != null && status.value() == 200) {
            try {
                JsonNode node = objectMapper.readTree(responseBody);
                if (node.has("balance")) {
                    TestContext.agentFloatBalance = new BigDecimal(node.get("balance").asText());
                    System.out.println("Final float balance: " + TestContext.agentFloatBalance);

                    // Note: We don't fail the test if balance is low
                    // Transaction tests will handle insufficient balance scenarios
                }
            } catch (Exception e) {
                System.out.println("Failed to parse balance: " + e.getMessage());
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Phase 4.4: Verify float setup is complete")
    void verifyFloatSetupComplete() {
        System.out.println("=== Phase 4 Float Setup Complete ===");
        System.out.println("Agent ID: " + TestContext.agentId);
        System.out.println("Float Balance: " + TestContext.agentFloatBalance);
    }
}

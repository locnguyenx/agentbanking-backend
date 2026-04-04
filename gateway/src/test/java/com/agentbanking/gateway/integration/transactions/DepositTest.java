package com.agentbanking.gateway.integration.transactions;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5b: Cash Deposit Tests
 * 
 * Tests the deposit transaction flow through the gateway.
 * Replaces scripts/e2e-tests/04-cash-deposit.sh
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(11)
class DepositTest extends BaseIntegrationTest {

    // ================================================================
    // BDD-D01: Successful Cash Deposit
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("BDD-D01 [HP]: Successful cash deposit")
    void successfulDeposit() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "amount": 200.00,
                    "currency": "MYR",
                    "customerAccount": "1234567890",
                    "customerName": "Test Customer"
                }
                """;

        var response = gatewayPost("/api/v1/deposit", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status, "Response status should not be null");
        assertEquals(200, status.value(), "Deposit should return 200");

        assertNotNull(responseBody, "Response body should not be null");
        JsonNode body;
        try {
            body = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            fail("Failed to parse deposit response: " + e.getMessage());
            return;
        }
        assertEquals("SUCCESS", body.get("status").asText(), "Status should be SUCCESS");
        assertNotNull(body.get("transactionId"), "Transaction ID should exist");
    }

    // ================================================================
    // BDD-D01-EC-02: Deposit without auth
    // ================================================================

    @Test
    @Order(2)
    @DisplayName("BDD-D01-EC-02: Deposit without auth returns 401")
    void deposit_withoutAuth_returns401() {
        String requestBody = """
                {
                    "amount": 100.00,
                    "currency": "MYR",
                    "customerAccount": "1234567890"
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/deposit", requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        assertNotNull(status);
        assertEquals(401, status.value(), "Should return 401 without auth");
    }

    // ================================================================
    // BDD-D01-EC-03: Duplicate idempotency
    // ================================================================

    @Test
    @Order(3)
    @DisplayName("BDD-D01-EC-03: Duplicate idempotency key")
    void deposit_duplicateIdempotency() {
        assumePhase2Complete();

        String idempotencyKey = "deposit-dup-" + idempotencyKey();
        String requestBody = """
                {
                    "amount": 75.00,
                    "currency": "MYR",
                    "customerAccount": "9876543210"
                }
                """;

        // First request
        var response1 = gatewayClient.post()
                .uri("/api/v1/deposit")
                .header("Authorization", "Bearer " + TestContext.agentToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange();

        var status1 = response1.expectBody(String.class).returnResult().getStatus();
        System.out.println("First deposit status: " + (status1 != null ? status1.value() : "null"));

        // Second request
        var response2 = gatewayClient.post()
                .uri("/api/v1/deposit")
                .header("Authorization", "Bearer " + TestContext.agentToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange();

        var status2 = response2.expectBody(String.class).returnResult().getStatus();
        System.out.println("Second deposit status: " + (status2 != null ? status2.value() : "null"));

        if (status1 != null && status2 != null) {
            assertEquals(status1.value(), status2.value(),
                    "Idempotent requests should return same status");
        }
    }

    private void assumePhase2Complete() {
        assertNotNull(TestContext.agentToken, "Phase 1 must be complete");
        assertNotNull(TestContext.agentId, "Phase 2 must be complete");
    }
}

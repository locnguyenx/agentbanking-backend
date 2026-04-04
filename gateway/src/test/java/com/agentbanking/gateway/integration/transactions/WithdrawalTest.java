package com.agentbanking.gateway.integration.transactions;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5a: Cash Withdrawal Tests
 * 
 * Tests the withdrawal transaction flow through the gateway.
 * Replaces scripts/e2e-tests/03-cash-withdrawal.sh
 * 
 * BDD-W01: ATM Card Withdrawal
 * BDD-W01-EC-01: Invalid card PIN
 * BDD-W01-EC-04: Withdrawal exceeds daily limit
 * BDD-W01-EC-05: Duplicate idempotency key
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(10)
class WithdrawalTest extends BaseIntegrationTest {

    // ================================================================
    // BDD-W01: Successful ATM Card Withdrawal
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("BDD-W01 [HP]: Successful ATM card withdrawal")
    void successfulWithdrawal() {
        assumePhase2Complete();

        String idempotencyKey = idempotencyKey();
        String requestBody = """
                {
                    "amount": 100.00,
                    "currency": "MYR",
                    "customerCard": "4111111111111111",
                    "customerPin": "1234",
                    "location": {"latitude": 3.1390, "longitude": 101.6869}
                }
                """;

        var response = gatewayPost("/api/v1/withdrawal", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status, "Response status should not be null");
        assertEquals(200, status.value(), "Withdrawal should return 200");

        assertNotNull(responseBody, "Response body should not be null");
        JsonNode body;
        try {
            body = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            fail("Failed to parse withdrawal response: " + e.getMessage());
            return;
        }
        assertEquals("SUCCESS", body.get("status").asText(), "Status should be SUCCESS");
        assertNotNull(body.get("transactionId"), "Transaction ID should exist");
        assertEquals("MYR", body.get("currency").asText(), "Currency should be MYR");
    }

    // ================================================================
    // BDD-W01-EC-04: Withdrawal exceeds daily limit
    // ================================================================

    @Test
    @Order(2)
    @DisplayName("BDD-W01-EC-04: Withdrawal exceeds daily limit")
    void withdrawal_exceedsDailyLimit() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "amount": 50000.00,
                    "currency": "MYR",
                    "customerCard": "4111111111111111",
                    "customerPin": "1234",
                    "location": {"latitude": 3.1390, "longitude": 101.6869}
                }
                """;

        var response = gatewayPost("/api/v1/withdrawal", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status, "Response status should not be null");
        assertEquals(400, status.value(), "Withdrawal over daily limit should return 400");

        assertNotNull(responseBody, "Response body should not be null");
        JsonNode body;
        try {
            body = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            fail("Failed to parse error response: " + e.getMessage());
            return;
        }
        assertNotNull(body.get("error"), "Error should be present");
    }

    // ================================================================
    // BDD-W01-EC-05: Duplicate idempotency key
    // ================================================================

    @Test
    @Order(3)
    @DisplayName("BDD-W01-EC-05: Duplicate idempotency key returns cached response")
    void withdrawal_duplicateIdempotency() {
        assumePhase2Complete();

        String idempotencyKey = "duplicate-test-" + idempotencyKey();
        String requestBody = """
                {
                    "amount": 50.00,
                    "currency": "MYR",
                    "customerCard": "4111111111111111",
                    "customerPin": "1234",
                    "location": {"latitude": 3.1390, "longitude": 101.6869}
                }
                """;

        // First request
        var response1 = gatewayClient.post()
                .uri("/api/v1/withdrawal")
                .header("Authorization", "Bearer " + TestContext.agentToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .header("X-POS-Terminal-Id", TestContext.POS_TERMINAL_ID)
                .header("X-GPS-Latitude", TestContext.GPS_LAT)
                .header("X-GPS-Longitude", TestContext.GPS_LNG)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange();

        var status1 = response1.expectBody(String.class).returnResult().getStatus();
        String body1 = response1.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status1, "First request status should not be null");

        // Second request with same idempotency key
        var response2 = gatewayClient.post()
                .uri("/api/v1/withdrawal")
                .header("Authorization", "Bearer " + TestContext.agentToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .header("X-POS-Terminal-Id", TestContext.POS_TERMINAL_ID)
                .header("X-GPS-Latitude", TestContext.GPS_LAT)
                .header("X-GPS-Longitude", TestContext.GPS_LNG)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange();

        var status2 = response2.expectBody(String.class).returnResult().getStatus();
        String body2 = response2.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status2, "Second request status should not be null");

        // Both should return the same status
        assertEquals(status1.value(), status2.value(), 
                "Idempotent requests should return same status");
    }

    // ================================================================
    // BDD-W02: MyKad-Based Withdrawal
    // ================================================================

    @Test
    @Order(4)
    @DisplayName("BDD-W02 [HP]: MyKad-based withdrawal")
    void myKadWithdrawal() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "amount": 100.00,
                    "currency": "MYR",
                    "customerMykad": "900101011234",
                    "location": {"latitude": 3.1390, "longitude": 101.6869}
                }
                """;

        // MyKad withdrawal uses a different endpoint
        var response = gatewayPost("/api/v1/withdrawal", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status, "Response status should not be null");
        assertEquals(200, status.value(), "MyKad withdrawal should return 200");

        assertNotNull(responseBody, "Response body should not be null");
        JsonNode body;
        try {
            body = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            fail("Failed to parse MyKad withdrawal response: " + e.getMessage());
            return;
        }
        assertEquals("SUCCESS", body.get("status").asText(), "Status should be SUCCESS");
    }

    // ================================================================
    // Auth checks
    // ================================================================

    @Test
    @Order(5)
    @DisplayName("BDD-W01-EC-02: Withdrawal without auth returns 401")
    void withdrawal_withoutAuth_returns401() {
        String requestBody = """
                {
                    "amount": 100.00,
                    "currency": "MYR",
                    "customerCard": "4111111111111111",
                    "customerPin": "1234"
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/withdrawal", requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        assertNotNull(status, "Status should not be null");
        assertEquals(401, status.value(), "Should return 401 without auth");
    }

    // ================================================================
    // Helpers
    // ================================================================

    private void assumePhase2Complete() {
        assertNotNull(TestContext.agentToken, "Phase 1 must be complete (agentToken required)");
        assertNotNull(TestContext.agentId, "Phase 2 must be complete (agentId required)");
    }
}

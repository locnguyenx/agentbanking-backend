package com.agentbanking.gateway.integration.transactions;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5f: DuitNow Transfer Tests
 * 
 * Replaces scripts/e2e-tests/08-duitnow-jompay.sh (duitnow section)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(15)
class DuitNowTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-DN01 [HP]: Successful DuitNow transfer")
    void successfulDuitNow() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "amount": 50.00,
                    "currency": "MYR",
                    "destinationBank": "MBBEMYKL",
                    "destinationAccount": "1234567890",
                    "recipientName": "Test Recipient"
                }
                """;

        var response = gatewayPost("/api/v1/transfer/duitnow", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status, "Response status should not be null");
        assertEquals(200, status.value(), "DuitNow transfer should return 200");

        assertNotNull(responseBody, "Response body should not be null");
        JsonNode body;
        try {
            body = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            fail("Failed to parse DuitNow response: " + e.getMessage());
            return;
        }
        assertEquals("SUCCESS", body.get("status").asText(), "Status should be SUCCESS");
        assertNotNull(body.get("transactionId"), "Transaction ID should exist");
    }

    @Test
    @Order(2)
    @DisplayName("BDD-DN01-EC-02: DuitNow without auth returns 401")
    void duitNow_withoutAuth_returns401() {
        String requestBody = """
                {
                    "amount": 50.00,
                    "destinationBank": "MBBEMYKL",
                    "destinationAccount": "1234567890"
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/transfer/duitnow", requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }

    private void assumePhase2Complete() {
        Assertions.assertNotNull(TestContext.agentToken);
        Assertions.assertNotNull(TestContext.agentId);
    }
}

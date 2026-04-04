package com.agentbanking.gateway.integration.transactions;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5h: eSSP (Education Savings) Purchase Tests
 * 
 * Replaces scripts/e2e-tests/09-ewallet-essp.sh (essp section)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(17)
class EsspTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-ESSP01 [HP]: Successful eSSP purchase")
    void successfulEsspPurchase() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "productCode": "SKIM_SIMPANAN_PENDIDIKAN",
                    "amount": 100.00,
                    "currency": "MYR",
                    "customerMobile": "0123456789"
                }
                """;

        var response = gatewayPost("/api/v1/essp/purchase", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status, "Response status should not be null");
        assertEquals(200, status.value(), "eSSP purchase should return 200");

        assertNotNull(responseBody, "Response body should not be null");
        JsonNode body;
        try {
            body = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            fail("Failed to parse eSSP response: " + e.getMessage());
            return;
        }
        assertEquals("SUCCESS", body.get("status").asText(), "Status should be SUCCESS");
        assertNotNull(body.get("transactionId"), "Transaction ID should exist");
    }

    @Test
    @Order(2)
    @DisplayName("BDD-ESSP01-EC-02: eSSP without auth returns 401")
    void essp_withoutAuth_returns401() {
        String requestBody = """
                {
                    "productCode": "SKIM_SIMPANAN_PENDIDIKAN",
                    "amount": 100.00
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/essp/purchase", requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }

    private void assumePhase2Complete() {
        Assertions.assertNotNull(TestContext.agentToken);
        Assertions.assertNotNull(TestContext.agentId);
    }
}

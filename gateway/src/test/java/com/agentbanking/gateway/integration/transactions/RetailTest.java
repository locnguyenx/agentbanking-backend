package com.agentbanking.gateway.integration.transactions;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5i: Retail / Merchant Tests
 * 
 * Tests retail sale, PIN purchase, and cash-back through the gateway.
 * Replaces scripts/e2e-tests/12-merchant-services.sh
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(18)
class RetailTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-R01 [HP]: Successful retail sale")
    void successfulRetailSale() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "amount": 75.00,
                    "currency": "MYR",
                    "merchantId": "%s"
                }
                """.formatted(TestContext.agentId);

        var response = gatewayPost("/api/v1/retail/sale", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status, "Response status should not be null");
        assertEquals(200, status.value(), "Retail sale should return 200");

        assertNotNull(responseBody, "Response body should not be null");
        JsonNode body;
        try {
            body = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            fail("Failed to parse retail sale response: " + e.getMessage());
            return;
        }
        assertEquals("SUCCESS", body.get("status").asText(), "Status should be SUCCESS");
        assertNotNull(body.get("transactionId"), "Transaction ID should exist");
    }

    @Test
    @Order(2)
    @DisplayName("BDD-R02 [HP]: Successful PIN purchase")
    void successfulPinPurchase() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "amount": 25.00,
                    "currency": "MYR"
                }
                """;

        var response = gatewayPost("/api/v1/retail/pin-purchase", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status, "Response status should not be null");
        assertEquals(200, status.value(), "PIN purchase should return 200");

        assertNotNull(responseBody, "Response body should not be null");
        JsonNode body;
        try {
            body = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            fail("Failed to parse PIN purchase response: " + e.getMessage());
            return;
        }
        assertEquals("SUCCESS", body.get("status").asText(), "Status should be SUCCESS");
        assertNotNull(body.get("transactionId"), "Transaction ID should exist");
    }

    @Test
    @Order(3)
    @DisplayName("BDD-R03 [HP]: Successful cash-back")
    void successfulCashBack() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "amount": 50.00,
                    "cashBackAmount": 20.00,
                    "currency": "MYR"
                }
                """;

        var response = gatewayPost("/api/v1/retail/cashback", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status, "Response status should not be null");
        assertEquals(200, status.value(), "Cash-back should return 200");

        assertNotNull(responseBody, "Response body should not be null");
        JsonNode body;
        try {
            body = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            fail("Failed to parse cash-back response: " + e.getMessage());
            return;
        }
        assertEquals("SUCCESS", body.get("status").asText(), "Status should be SUCCESS");
        assertNotNull(body.get("transactionId"), "Transaction ID should exist");
    }

    @Test
    @Order(4)
    @DisplayName("BDD-R01-EC-02: Retail sale without auth returns 401")
    void retailSale_withoutAuth_returns401() {
        String requestBody = """
                {
                    "amount": 75.00
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/retail/sale", requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }

    private void assumePhase2Complete() {
        Assertions.assertNotNull(TestContext.agentToken);
        Assertions.assertNotNull(TestContext.agentId);
    }
}

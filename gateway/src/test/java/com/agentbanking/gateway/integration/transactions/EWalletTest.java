package com.agentbanking.gateway.integration.transactions;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5g: E-Wallet Tests
 * 
 * Tests e-wallet withdrawal and top-up through the gateway.
 * Replaces scripts/e2e-tests/09-ewallet-essp.sh (ewallet section)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(16)
class EWalletTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-EW01 [HP]: Successful e-wallet withdrawal")
    void successfulEWalletWithdrawal() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "walletProvider": "SARAWAK_PAY",
                    "walletAccountId": "SP-12345",
                    "amount": 50.00,
                    "currency": "MYR"
                }
                """;

        var response = gatewayPost("/api/v1/ewallet/withdraw", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status, "Response status should not be null");
        assertEquals(200, status.value(), "E-wallet withdrawal should return 200");

        assertNotNull(responseBody, "Response body should not be null");
        JsonNode body;
        try {
            body = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            fail("Failed to parse e-wallet withdrawal response: " + e.getMessage());
            return;
        }
        assertEquals("SUCCESS", body.get("status").asText(), "Status should be SUCCESS");
        assertNotNull(body.get("transactionId"), "Transaction ID should exist");
    }

    @Test
    @Order(2)
    @DisplayName("BDD-EW02 [HP]: Successful e-wallet top-up")
    void successfulEWalletTopup() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "walletProvider": "SARAWAK_PAY",
                    "walletAccountId": "SP-12345",
                    "amount": 25.00,
                    "currency": "MYR"
                }
                """;

        var response = gatewayPost("/api/v1/ewallet/topup", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        assertNotNull(status, "Response status should not be null");
        assertEquals(200, status.value(), "E-wallet topup should return 200");

        assertNotNull(responseBody, "Response body should not be null");
        JsonNode body;
        try {
            body = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            fail("Failed to parse e-wallet topup response: " + e.getMessage());
            return;
        }
        assertEquals("SUCCESS", body.get("status").asText(), "Status should be SUCCESS");
        assertNotNull(body.get("transactionId"), "Transaction ID should exist");
    }

    @Test
    @Order(3)
    @DisplayName("BDD-EW01-EC-02: E-wallet withdrawal without auth returns 401")
    void eWallet_withoutAuth_returns401() {
        String requestBody = """
                {
                    "walletProvider": "SARAWAK_PAY",
                    "walletAccountId": "SP-12345",
                    "amount": 50.00
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/ewallet/withdraw", requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }

    private void assumePhase2Complete() {
        Assertions.assertNotNull(TestContext.agentToken);
        Assertions.assertNotNull(TestContext.agentId);
    }
}

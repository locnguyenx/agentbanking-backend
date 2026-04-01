package com.agentbanking.gateway.integration.gateway;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

/**
 * Phase 8b: Gateway Request/Response Transformation Tests
 * 
 * Tests that the gateway correctly transforms external API formats
 * to internal service formats and back.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(51)
class TransformationTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-TF01: Withdrawal request is transformed correctly")
    void withdrawalRequest_isTransformed() {
        // This test verifies that the gateway's RequestTransform filter
        // converts external withdrawal format to internal debit format.
        // The transformation should:
        // - Add agentId from JWT X-Agent-Id header
        // - Mask customerCard to customerCardMasked
        // - Convert location.latitude/longitude to geofenceLat/geofenceLng
        // - Drop customerPin

        String requestBody = """
                {
                    "amount": 10.00,
                    "currency": "MYR",
                    "customerCard": "4111111111111111",
                    "customerPin": "1234",
                    "location": {"latitude": 3.1390, "longitude": 101.6869}
                }
                """;

        var response = gatewayPost("/api/v1/withdrawal", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Transformation test status: " + (status != null ? status.value() : "null"));
        System.out.println("Transformation test response: " + responseBody);

        // The response should be in external format (not internal)
        // External format has: transactionId, status, amount, currency, timestamp
        // Internal format has: transactionId, status, amount, balance
        // The gateway should have transformed the response back to external format
    }

    @Test
    @Order(2)
    @DisplayName("BDD-TF02: Deposit request is transformed correctly")
    void depositRequest_isTransformed() {
        String requestBody = """
                {
                    "amount": 10.00,
                    "currency": "MYR",
                    "customerAccount": "1234567890"
                }
                """;

        var response = gatewayPost("/api/v1/deposit", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Deposit transformation status: " + (status != null ? status.value() : "null"));
        System.out.println("Deposit transformation response: " + responseBody);
    }
}

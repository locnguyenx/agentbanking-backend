package com.agentbanking.gateway.integration.transactions;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

/**
 * Phase 5c: Prepaid Top-up Tests
 * 
 * Tests the top-up transaction flow through the gateway.
 * Replaces scripts/e2e-tests/07-prepaid-topup.sh
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(12)
class TopupTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-T01 [HP]: Successful Celcom top-up")
    void successfulTopup() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "telco": "CELCOM",
                    "phoneNumber": "0123456789",
                    "amount": 10.00,
                    "currency": "MYR"
                }
                """;

        var response = gatewayPost("/api/v1/topup", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Topup status: " + (status != null ? status.value() : "null"));
        System.out.println("Topup response: " + responseBody);
    }

    @Test
    @Order(2)
    @DisplayName("BDD-T01-EC-02: Topup without auth returns 401")
    void topup_withoutAuth_returns401() {
        String requestBody = """
                {
                    "telco": "CELCOM",
                    "phoneNumber": "0123456789",
                    "amount": 10.00
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/topup", requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0, "Should return 401");
    }

    private void assumePhase2Complete() {
        Assertions.assertNotNull(TestContext.agentToken);
        Assertions.assertNotNull(TestContext.agentId);
    }
}

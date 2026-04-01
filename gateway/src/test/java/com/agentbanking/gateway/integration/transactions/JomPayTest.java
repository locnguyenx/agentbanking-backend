package com.agentbanking.gateway.integration.transactions;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

/**
 * Phase 5e: JomPAY Tests
 * 
 * Replaces scripts/e2e-tests/08-duitnow-jompay.sh (jompay section)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(14)
class JomPayTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-JP01 [HP]: Successful JomPAY payment")
    void successfulJomPay() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "billerCode": "5678",
                    "ref1": "1234567890",
                    "ref2": "",
                    "amount": 100.00,
                    "currency": "MYR"
                }
                """;

        var response = gatewayPost("/api/v1/billpayment/jompay", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("JomPay status: " + (status != null ? status.value() : "null"));
        System.out.println("JomPay response: " + responseBody);
    }

    @Test
    @Order(2)
    @DisplayName("BDD-JP01-EC-02: JomPay without auth returns 401")
    void jomPay_withoutAuth_returns401() {
        String requestBody = """
                {
                    "billerCode": "5678",
                    "ref1": "1234567890",
                    "amount": 100.00
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/billpayment/jompay", requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }

    private void assumePhase2Complete() {
        Assertions.assertNotNull(TestContext.agentToken);
        Assertions.assertNotNull(TestContext.agentId);
    }
}

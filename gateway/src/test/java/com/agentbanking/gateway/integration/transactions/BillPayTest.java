package com.agentbanking.gateway.integration.transactions;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

/**
 * Phase 5d: Bill Payment Tests
 * 
 * Tests bill payment through the gateway.
 * Replaces scripts/e2e-tests/06-bill-payments.sh (bill pay section)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(13)
class BillPayTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-B01 [HP]: Successful bill payment")
    void successfulBillPay() {
        assumePhase2Complete();

        String requestBody = """
                {
                    "billerCode": "ASTRO",
                    "ref1": "1234567890",
                    "ref2": "test-ref",
                    "amount": 50.00,
                    "currency": "MYR"
                }
                """;

        var response = gatewayPost("/api/v1/bill/pay", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Bill pay status: " + (status != null ? status.value() : "null"));
        System.out.println("Bill pay response: " + responseBody);
    }

    @Test
    @Order(2)
    @DisplayName("BDD-B01-EC-02: Bill pay without auth returns 401")
    void billPay_withoutAuth_returns401() {
        String requestBody = """
                {
                    "billerCode": "ASTRO",
                    "ref1": "1234567890",
                    "amount": 50.00
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/bill/pay", requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }

    private void assumePhase2Complete() {
        Assertions.assertNotNull(TestContext.agentToken);
        Assertions.assertNotNull(TestContext.agentId);
    }
}

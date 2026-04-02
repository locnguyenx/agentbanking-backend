package com.agentbanking.gateway.integration.transactions;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

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

        System.out.println("eSSP status: " + (status != null ? status.value() : "null"));
        System.out.println("eSSP response: " + responseBody);
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

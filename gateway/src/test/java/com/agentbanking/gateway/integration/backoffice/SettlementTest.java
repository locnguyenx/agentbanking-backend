package com.agentbanking.gateway.integration.backoffice;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

/**
 * Phase 7b: Settlement Tests
 * 
 * Tests settlement endpoints through the ledger service.
 * Replaces scripts/e2e-tests/14-eod-settlement.sh
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(41)
class SettlementTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-ST01 [HP]: Get settlement for today")
    void getSettlementForToday() {
        String today = LocalDate.now().toString();

        var response = gatewayGet("/api/v1/backoffice/settlement?date=" + today, 
                TestContext.adminToken);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Settlement status: " + (status != null ? status.value() : "null"));
        System.out.println("Settlement response: " + responseBody);
    }

    @Test
    @Order(2)
    @DisplayName("BDD-ST02: Get settlement for yesterday")
    void getSettlementForYesterday() {
        String yesterday = LocalDate.now().minusDays(1).toString();

        var response = gatewayGet("/api/v1/backoffice/settlement?date=" + yesterday, 
                TestContext.adminToken);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Yesterday settlement status: " + (status != null ? status.value() : "null"));
        System.out.println("Yesterday settlement response: " + responseBody);
    }

    @Test
    @Order(3)
    @DisplayName("BDD-ST01-EC-02: Settlement without auth returns 401")
    void settlement_withoutAuth_returns401() {
        var response = gatewayGetNoAuth("/api/v1/backoffice/settlement?date=2026-03-31");

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }
}

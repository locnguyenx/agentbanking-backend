package com.agentbanking.gateway.integration.backoffice;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

/**
 * Phase 7c: Discrepancy / Reconciliation Tests
 * 
 * Tests discrepancy resolution through the ledger service.
 * Replaces scripts/e2e-tests/15-discrepancy-resolution.sh
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(42)
class DiscrepancyTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-DISC01: Maker proposes discrepancy resolution")
    void makerProposesResolution() {
        String caseId = "DISC-001";
        String body = """
                {
                    "action": "PROPOSE",
                    "notes": "Ghost transaction identified, propose reversal",
                    "adjustmentAmount": 100.00
                }
                """;

        var response = gatewayPost("/api/v1/backoffice/discrepancy/" + caseId + "/maker-action",
                TestContext.makerToken, body);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Maker action status: " + (status != null ? status.value() : "null"));
        System.out.println("Maker action response: " + responseBody);
    }

    @Test
    @Order(2)
    @DisplayName("BDD-DISC02: Checker approves discrepancy resolution")
    void checkerApprovesResolution() {
        String caseId = "DISC-001";
        String body = """
                {
                    "action": "APPROVE",
                    "notes": "Approved after review"
                }
                """;

        var response = gatewayPost("/api/v1/backoffice/discrepancy/" + caseId + "/checker-approve",
                TestContext.checkerToken, body);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Checker approve status: " + (status != null ? status.value() : "null"));
        System.out.println("Checker approve response: " + responseBody);
    }

    @Test
    @Order(3)
    @DisplayName("BDD-DISC03: Checker rejects discrepancy resolution")
    void checkerRejectsResolution() {
        String caseId = "DISC-002";
        String body = """
                {
                    "action": "REJECT",
                    "notes": "Insufficient evidence"
                }
                """;

        var response = gatewayPost("/api/v1/backoffice/discrepancy/" + caseId + "/checker-reject",
                TestContext.checkerToken, body);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Checker reject status: " + (status != null ? status.value() : "null"));
        System.out.println("Checker reject response: " + responseBody);
    }

    @Test
    @Order(4)
    @DisplayName("BDD-DISC01-EC-02: Discrepancy without auth returns 401")
    void discrepancy_withoutAuth_returns401() {
        String body = """
                {
                    "action": "PROPOSE",
                    "notes": "Test"
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/backoffice/discrepancy/DISC-001/maker-action", body);

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }
}

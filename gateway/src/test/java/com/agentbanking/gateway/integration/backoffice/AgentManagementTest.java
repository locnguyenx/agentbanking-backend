package com.agentbanking.gateway.integration.backoffice;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

/**
 * Phase 7d: Agent Management Tests
 * 
 * Tests agent CRUD operations through the onboarding service.
 * Replaces scripts/e2e-tests/17-backoffice.sh (agent section)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(43)
class AgentManagementTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-AM01 [HP]: List all agents")
    void listAllAgents() {
        var response = listAgents(TestContext.adminToken);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("List agents status: " + (status != null ? status.value() : "null"));
        System.out.println("List agents response: " + responseBody);
    }

    @Test
    @Order(2)
    @DisplayName("BDD-AM02 [HP]: Get agent by ID")
    void getAgentById() {
        if (TestContext.agentId == null) {
            System.out.println("Skipping - no agentId from Phase 2");
            return;
        }

        var response = getAgent(TestContext.agentId, TestContext.adminToken);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Get agent status: " + (status != null ? status.value() : "null"));
        System.out.println("Get agent response: " + responseBody);
    }

    @Test
    @Order(3)
    @DisplayName("BDD-AM03: Update agent")
    void updateAgent() {
        if (TestContext.agentId == null) {
            System.out.println("Skipping - no agentId from Phase 2");
            return;
        }

        String body = """
                {
                    "businessName": "Updated E2E Test Business",
                    "tier": "STANDARD",
                    "merchantGpsLat": 3.1390,
                    "merchantGpsLng": 101.6869,
                    "phoneNumber": "0123456789"
                }
                """;

        var response = gatewayPut("/api/v1/backoffice/agents/" + TestContext.agentId,
                TestContext.adminToken, body);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Update agent status: " + (status != null ? status.value() : "null"));
        System.out.println("Update agent response: " + responseBody);
    }

    @Test
    @Order(4)
    @DisplayName("BDD-AM01-EC-02: List agents without auth returns 401")
    void listAgents_withoutAuth_returns401() {
        var response = gatewayGetNoAuth("/api/v1/backoffice/agents");

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }
}

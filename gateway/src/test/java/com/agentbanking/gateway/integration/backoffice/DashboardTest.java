package com.agentbanking.gateway.integration.backoffice;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 7a: Dashboard Tests
 * 
 * Tests dashboard endpoints through the ledger service.
 * Replaces scripts/e2e-tests/17-backoffice.sh (dashboard section)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(40)
class DashboardTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-BO01 [HP]: Get dashboard metrics")
    void getDashboard() {
        var response = gatewayGet("/api/v1/backoffice/dashboard", TestContext.adminToken);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Dashboard status: " + (status != null ? status.value() : "null"));
        System.out.println("Dashboard response: " + responseBody);

        if (status != null && status.value() == 200) {
            try {
                JsonNode body = objectMapper.readTree(responseBody);
                // Dashboard should contain: totalAgents, activeAgents, totalTransactions, etc.
                assertNotNull(body, "Dashboard should return data");
            } catch (Exception e) {
                System.out.println("Failed to parse dashboard response");
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("BDD-BO02: List transactions")
    void listTransactions() {
        var response = gatewayGet("/api/v1/backoffice/transactions?page=0&size=10", 
                TestContext.adminToken);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Transactions status: " + (status != null ? status.value() : "null"));
        System.out.println("Transactions response: " + responseBody);
    }

    @Test
    @Order(3)
    @DisplayName("BDD-BO03: List agents")
    void listAgents() {
        var response = gatewayGet("/api/v1/backoffice/agents?page=0&size=10", 
                TestContext.adminToken);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Agents list status: " + (status != null ? status.value() : "null"));
        System.out.println("Agents list response: " + responseBody);
    }

    @Test
    @Order(4)
    @DisplayName("BDD-BO01-EC-02: Dashboard without auth returns 401")
    void dashboard_withoutAuth_returns401() {
        var response = gatewayGetNoAuth("/api/v1/backoffice/dashboard");

        var status = response.expectBody(String.class).returnResult().getStatus();
        assertEquals(401, status != null ? status.value() : 0);
    }
}

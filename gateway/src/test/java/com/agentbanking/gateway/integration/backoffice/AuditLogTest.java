package com.agentbanking.gateway.integration.backoffice;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

/**
 * Phase 7e: Audit Log Tests
 * 
 * Tests audit log endpoints through the auth service.
 * Replaces scripts/e2e-tests/17-backoffice.sh (audit section)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(44)
class AuditLogTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-AUD01 [HP]: Get audit logs")
    void getAuditLogs() {
        var response = gatewayGet("/api/v1/backoffice/audit-logs?page=0&size=20",
                TestContext.adminToken);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Audit logs status: " + (status != null ? status.value() : "null"));
        System.out.println("Audit logs response: " + responseBody);
    }

    @Test
    @Order(2)
    @DisplayName("BDD-AUD02: Get audit logs with filter")
    void getAuditLogsWithFilter() {
        var response = gatewayGet("/api/v1/backoffice/audit-logs?entityType=USER&page=0&size=10",
                TestContext.adminToken);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Filtered audit logs status: " + (status != null ? status.value() : "null"));
        System.out.println("Filtered audit logs response: " + responseBody);
    }

    @Test
    @Order(3)
    @DisplayName("BDD-AUD01-EC-02: Audit logs without auth returns 401")
    void auditLogs_withoutAuth_returns401() {
        var response = gatewayGetNoAuth("/api/v1/backoffice/audit-logs");

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }
}

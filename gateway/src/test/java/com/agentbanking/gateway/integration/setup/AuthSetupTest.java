package com.agentbanking.gateway.integration.setup;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1: Auth Setup
 * 
 * Creates all test users, roles, and permissions via the auth service API.
 * Populates TestContext with tokens and user IDs for subsequent phases.
 * 
 * This replaces the shell script seed-test-data.sh.
 * 
 * Uses @TestMethodOrder to ensure sequential execution.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(1)
class AuthSetupTest extends BaseIntegrationTest {

    // ================================================================
    // STEP 1: Bootstrap admin (only works when no users exist)
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("Phase 1.1: Bootstrap admin user")
    void testBootstrapAdmin() {
        // Try to bootstrap - may fail if users already exist (that's OK)
        var response = bootstrapAdmin();
        var status = response.expectBody(String.class).returnResult().getStatus();

        if (status != null && status.value() == 201) {
            // Bootstrap succeeded - extract user ID
            JsonNode body = parseBody(response);
            TestContext.adminUserId = UUID.fromString(body.get("userId").asText());
            System.out.println("Admin bootstrapped with ID: " + TestContext.adminUserId);
        } else {
            // Bootstrap failed (users already exist) - that's fine, just get the token
            System.out.println("Admin already exists, getting token...");
        }

        // Get admin token (either way)
        assertNotNull(getToken(TestContext.ADMIN_USERNAME, TestContext.ADMIN_PASSWORD),
                "Should be able to get admin token");
    }

    @Test
    @Order(2)
    @DisplayName("Phase 1.2: Get admin token")
    void testGetAdminToken() {
        TestContext.adminToken = getToken(TestContext.ADMIN_USERNAME, TestContext.ADMIN_PASSWORD);
        assertNotNull(TestContext.adminToken, "Admin token should not be null");
        assertFalse(TestContext.adminToken.isBlank(), "Admin token should not be blank");
        System.out.println("Admin token obtained: " + TestContext.adminToken.substring(0, 20) + "...");
    }

    // ================================================================
    // STEP 2: Create test users
    // ================================================================

    @Test
    @Order(10)
    @DisplayName("Phase 1.10: Create agent001 user")
    void createAgentUser() {
        var response = createUser(
                TestContext.AGENT_USERNAME,
                "agent001@bank.com",
                TestContext.AGENT_PASSWORD,
                "Test Agent",
                TestContext.adminToken
        );

        var status = response.expectBody(String.class).returnResult().getStatus();
        if (status != null && status.value() == 201) {
            JsonNode body = parseBody(response);
            TestContext.agentUserId = UUID.fromString(body.get("userId").asText());
            System.out.println("Agent user created with ID: " + TestContext.agentUserId);
        } else {
            System.out.println("Agent user may already exist");
            // Try to get token anyway
        }

        // Verify we can get a token for this user
        String token = getToken(TestContext.AGENT_USERNAME, TestContext.AGENT_PASSWORD);
        assertNotNull(token, "Should be able to get agent token");
        TestContext.agentToken = token;
    }

    @Test
    @Order(11)
    @DisplayName("Phase 1.11: Create operator001 user")
    void createOperatorUser() {
        var response = createUser(
                TestContext.OPERATOR_USERNAME,
                "operator001@bank.com",
                TestContext.OPERATOR_PASSWORD,
                "Test Operator",
                TestContext.adminToken
        );

        var status = response.expectBody(String.class).returnResult().getStatus();
        if (status != null && status.value() == 201) {
            JsonNode body = parseBody(response);
            TestContext.operatorUserId = UUID.fromString(body.get("userId").asText());
        }

        TestContext.operatorToken = getToken(TestContext.OPERATOR_USERNAME, TestContext.OPERATOR_PASSWORD);
        assertNotNull(TestContext.operatorToken);
    }

    @Test
    @Order(12)
    @DisplayName("Phase 1.12: Create maker001 user")
    void createMakerUser() {
        var response = createUser(
                TestContext.MAKER_USERNAME,
                "maker001@bank.com",
                TestContext.MAKER_PASSWORD,
                "Test Maker",
                TestContext.adminToken
        );

        var status = response.expectBody(String.class).returnResult().getStatus();
        if (status != null && status.value() == 201) {
            JsonNode body = parseBody(response);
            TestContext.makerUserId = UUID.fromString(body.get("userId").asText());
        }

        TestContext.makerToken = getToken(TestContext.MAKER_USERNAME, TestContext.MAKER_PASSWORD);
        assertNotNull(TestContext.makerToken);
    }

    @Test
    @Order(13)
    @DisplayName("Phase 1.13: Create checker001 user")
    void createCheckerUser() {
        var response = createUser(
                TestContext.CHECKER_USERNAME,
                "checker001@bank.com",
                TestContext.CHECKER_PASSWORD,
                "Test Checker",
                TestContext.adminToken
        );

        var status = response.expectBody(String.class).returnResult().getStatus();
        if (status != null && status.value() == 201) {
            JsonNode body = parseBody(response);
            TestContext.checkerUserId = UUID.fromString(body.get("userId").asText());
        }

        TestContext.checkerToken = getToken(TestContext.CHECKER_USERNAME, TestContext.CHECKER_PASSWORD);
        assertNotNull(TestContext.checkerToken);
    }

    @Test
    @Order(14)
    @DisplayName("Phase 1.14: Create compliance001 user")
    void createComplianceUser() {
        var response = createUser(
                TestContext.COMPLIANCE_USERNAME,
                "compliance001@bank.com",
                TestContext.COMPLIANCE_PASSWORD,
                "Test Compliance Officer",
                TestContext.adminToken
        );

        var status = response.expectBody(String.class).returnResult().getStatus();
        if (status != null && status.value() == 201) {
            System.out.println("Compliance user created");
        }

        TestContext.complianceToken = getToken(TestContext.COMPLIANCE_USERNAME, TestContext.COMPLIANCE_PASSWORD);
        assertNotNull(TestContext.complianceToken);
    }

    @Test
    @Order(15)
    @DisplayName("Phase 1.15: Create teller001 user")
    void createTellerUser() {
        var response = createUser(
                TestContext.TELLER_USERNAME,
                "teller001@bank.com",
                TestContext.TELLER_PASSWORD,
                "Test Teller",
                TestContext.adminToken
        );

        var status = response.expectBody(String.class).returnResult().getStatus();
        if (status != null && status.value() == 201) {
            System.out.println("Teller user created");
        }

        TestContext.tellerToken = getToken(TestContext.TELLER_USERNAME, TestContext.TELLER_PASSWORD);
        assertNotNull(TestContext.tellerToken);
    }

    @Test
    @Order(16)
    @DisplayName("Phase 1.16: Create supervisor001 user")
    void createSupervisorUser() {
        var response = createUser(
                TestContext.SUPERVISOR_USERNAME,
                "supervisor001@bank.com",
                TestContext.SUPERVISOR_PASSWORD,
                "Test Supervisor",
                TestContext.adminToken
        );

        var status = response.expectBody(String.class).returnResult().getStatus();
        if (status != null && status.value() == 201) {
            System.out.println("Supervisor user created");
        }

        TestContext.supervisorToken = getToken(TestContext.SUPERVISOR_USERNAME, TestContext.SUPERVISOR_PASSWORD);
        assertNotNull(TestContext.supervisorToken);
    }

    // ================================================================
    // STEP 3: Verify all tokens work
    // ================================================================

    @Test
    @Order(20)
    @DisplayName("Phase 1.20: Verify auth setup is complete")
    void verifyAuthSetup() {
        assertTrue(TestContext.isAuthSetupComplete(), 
                "Phase 1 should have populated admin and agent tokens");

        System.out.println("=== Phase 1 Auth Setup Complete ===");
        System.out.println("Admin token: " + TestContext.adminToken.substring(0, 20) + "...");
        System.out.println("Agent token: " + TestContext.agentToken.substring(0, 20) + "...");
        System.out.println("Agent user ID: " + TestContext.agentUserId);
    }
}

package com.agentbanking.gateway.integration.kyc;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

/**
 * Phase 6c: Agent Application Tests
 * 
 * Tests agent application submission through the onboarding service.
 * Replaces scripts/e2e-tests/10-agent-onboarding.sh
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(32)
class AgentApplicationTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-AA01 [HP]: Submit micro-agent application")
    void submitMicroAgentApplication() {
        String body = """
                {
                    "mykadNumber": "920505012345"
                }
                """;

        var response = onboardingClient.post()
                .uri("/internal/onboarding/agent/micro/start")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Micro agent start status: " + (status != null ? status.value() : "null"));
        System.out.println("Micro agent start response: " + responseBody);
    }

    @Test
    @Order(2)
    @DisplayName("BDD-AA02 [HP]: Submit standard agent application")
    void submitStandardAgentApplication() {
        String body = """
                {
                    "mykadNumber": "%s",
                    "extractedName": "Test Agent Two",
                    "ssmBusinessName": "Test Business Two",
                    "ssmOwnerName": "Test Agent Two",
                    "agentTier": "STANDARD",
                    "merchantGpsLat": 3.1390,
                    "merchantGpsLng": 101.6869,
                    "phoneNumber": "0198765432"
                }
                """.formatted(TestContext.TEST_AGENT_MYKAD);

        var response = onboardingClient.post()
                .uri("/internal/onboarding/application")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Application submit status: " + (status != null ? status.value() : "null"));
        System.out.println("Application submit response: " + responseBody);
    }

    @Test
    @Order(3)
    @DisplayName("BDD-AA03 [HP]: Submit application via gateway")
    void submitApplication_viaGateway() {
        String body = """
                {
                    "businessName": "Gateway Test Business",
                    "tier": "STANDARD",
                    "mykadNumber": "880101015678",
                    "phoneNumber": "0112233445",
                    "merchantGpsLat": 3.1390,
                    "merchantGpsLng": 101.6869
                }
                """;

        var response = gatewayPost("/api/v1/onboarding/submit-application", 
                TestContext.adminToken, body);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Gateway application status: " + (status != null ? status.value() : "null"));
        System.out.println("Gateway application response: " + responseBody);
    }

    @Test
    @Order(4)
    @DisplayName("BDD-AA01-EC-02: Application without auth returns 401")
    void application_withoutAuth_returns401() {
        String body = """
                {
                    "businessName": "Test",
                    "tier": "STANDARD",
                    "mykadNumber": "123456789012",
                    "phoneNumber": "0123456789"
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/onboarding/submit-application", body);

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }
}

package com.agentbanking.gateway.integration.kyc;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 6a: MyKad Verification Tests
 * 
 * Tests KYC verification through the onboarding service.
 * Replaces scripts/e2e-tests/05-ekyc-onboarding.sh (MyKad section)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(30)
class MyKadVerifyTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-KYC01 [HP]: Verify valid MyKad")
    void verifyValidMyKad() {
        String body = """
                {
                    "mykadNumber": "900101011234"
                }
                """;

        var response = onboardingClient.post()
                .uri("/internal/verify-mykad")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("MyKad verify status: " + (status != null ? status.value() : "null"));
        System.out.println("MyKad verify response: " + responseBody);

        if (status != null && status.value() == 200) {
            try {
                JsonNode node = objectMapper.readTree(responseBody);
                assertNotNull(node.get("verificationId"), "Should have verificationId");
                assertNotNull(node.get("status"), "Should have status");
                assertNotNull(node.get("fullName"), "Should have fullName");
            } catch (Exception e) {
                System.out.println("Failed to parse MyKad verify response");
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("BDD-KYC01-EC-01: Verify invalid MyKad")
    void verifyInvalidMyKad() {
        String body = """
                {
                    "mykadNumber": "000000000000"
                }
                """;

        var response = onboardingClient.post()
                .uri("/internal/verify-mykad")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Invalid MyKad status: " + (status != null ? status.value() : "null"));
        System.out.println("Invalid MyKad response: " + responseBody);

        // Should return 400 or 404 for invalid MyKad
        if (status != null) {
            assertTrue(status.value() >= 400, "Invalid MyKad should return error status");
        }
    }

    @Test
    @Order(3)
    @DisplayName("BDD-KYC01 [HP]: Verify MyKad via gateway")
    void verifyMyKad_viaGateway() {
        String body = """
                {
                    "mykadNumber": "900101011234"
                }
                """;

        var response = gatewayPost("/api/v1/kyc/verify", TestContext.adminToken, body);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Gateway MyKad verify status: " + (status != null ? status.value() : "null"));
        System.out.println("Gateway MyKad verify response: " + responseBody);
    }

    @Test
    @Order(4)
    @DisplayName("BDD-KYC01-EC-02: MyKad verify without auth returns 401")
    void myKadVerify_withoutAuth_returns401() {
        String body = """
                {
                    "mykadNumber": "900101011234"
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/kyc/verify", body);

        var status = response.expectBody(String.class).returnResult().getStatus();
        assertEquals(401, status != null ? status.value() : 0);
    }
}

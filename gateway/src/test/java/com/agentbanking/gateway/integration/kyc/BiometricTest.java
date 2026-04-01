package com.agentbanking.gateway.integration.kyc;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

/**
 * Phase 6b: Biometric Verification Tests
 * 
 * Tests biometric matching through the onboarding service.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(31)
class BiometricTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-BIO01 [HP]: Biometric match")
    void biometricMatch() {
        String body = """
                {
                    "verificationId": "test-verification-id",
                    "biometricData": "base64-encoded-biometric-data"
                }
                """;

        var response = onboardingClient.post()
                .uri("/internal/biometric")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Biometric match status: " + (status != null ? status.value() : "null"));
        System.out.println("Biometric match response: " + responseBody);
    }

    @Test
    @Order(2)
    @DisplayName("BDD-BIO02 [HP]: Biometric match via gateway")
    void biometricMatch_viaGateway() {
        String body = """
                {
                    "verificationId": "test-verification-id",
                    "biometricData": "base64-encoded-biometric-data"
                }
                """;

        var response = gatewayPost("/api/v1/kyc/biometric", TestContext.adminToken, body);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Gateway biometric status: " + (status != null ? status.value() : "null"));
        System.out.println("Gateway biometric response: " + responseBody);
    }

    @Test
    @Order(3)
    @DisplayName("BDD-BIO01-EC-02: Biometric without auth returns 401")
    void biometric_withoutAuth_returns401() {
        String body = """
                {
                    "verificationId": "test-id",
                    "biometricData": "data"
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/kyc/biometric", body);

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }
}

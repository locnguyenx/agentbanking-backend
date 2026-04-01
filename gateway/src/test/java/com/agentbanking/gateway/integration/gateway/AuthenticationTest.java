package com.agentbanking.gateway.integration.gateway;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

/**
 * Phase 8a: Gateway Authentication Tests
 * 
 * Tests JWT authentication through the gateway.
 * Replaces scripts/e2e-tests/16-api-gateway.sh (auth section)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(50)
class AuthenticationTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-GW01: Request without auth header returns 401")
    void requestWithoutAuth_returns401() {
        String requestBody = """
                {
                    "amount": 100.00,
                    "currency": "MYR",
                    "customerCard": "4111111111111111",
                    "customerPin": "1234"
                }
                """;

        var response = gatewayPostNoAuth("/api/v1/withdrawal", requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0,
                "Should return 401 without Authorization header");
    }

    @Test
    @Order(2)
    @DisplayName("BDD-GW02: Request with invalid token returns 401")
    void requestWithInvalidToken_returns401() {
        String requestBody = """
                {
                    "amount": 100.00,
                    "currency": "MYR",
                    "customerCard": "4111111111111111",
                    "customerPin": "1234"
                }
                """;

        var response = gatewayClient.post()
                .uri("/api/v1/withdrawal")
                .header("Authorization", "Bearer invalid-token-here")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0,
                "Should return 401 with invalid token");
    }

    @Test
    @Order(3)
    @DisplayName("BDD-GW03: Request with malformed bearer token returns 401")
    void requestWithMalformedToken_returns401() {
        String requestBody = """
                {
                    "amount": 100.00,
                    "currency": "MYR"
                }
                """;

        // Missing "Bearer " prefix
        var response = gatewayClient.post()
                .uri("/api/v1/withdrawal")
                .header("Authorization", "not-a-bearer-token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0,
                "Should return 401 with malformed bearer token");
    }

    @Test
    @Order(4)
    @DisplayName("BDD-GW04: Valid token passes authentication")
    void validToken_passesAuthentication() {
        // Assume Phase 1 is complete
        Assertions.assertNotNull(TestContext.agentToken, "Phase 1 must set agentToken");

        String requestBody = """
                {
                    "amount": 10.00,
                    "currency": "MYR",
                    "customerCard": "4111111111111111",
                    "customerPin": "1234"
                }
                """;

        var response = gatewayPost("/api/v1/withdrawal", TestContext.agentToken, requestBody);

        var status = response.expectBody(String.class).returnResult().getStatus();
        // Should NOT be 401 - it may be 200, 400, or 500 but not 401
        Assertions.assertNotEquals(401, status != null ? status.value() : 0,
                "Valid token should not return 401");
    }

    @Test
    @Order(5)
    @DisplayName("BDD-GW05: Protected backoffice endpoint requires auth")
    void backofficeEndpoint_requiresAuth() {
        var response = gatewayGetNoAuth("/api/v1/backoffice/dashboard");

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0,
                "Backoffice endpoint should require auth");
    }
}

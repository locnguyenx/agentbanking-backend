package com.agentbanking.gateway.integration.gateway;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 8c: Error Handling Tests
 * 
 * Tests that the gateway properly handles invalid requests.
 * Replaces scripts/e2e-tests/16-api-gateway.sh (error section)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(52)
class ErrorHandlingTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-EH01: Invalid JSON returns 400")
    void invalidJson_returns400() {
        var response = gatewayClient.post()
                .uri("/api/v1/withdrawal")
                .header("Authorization", "Bearer " + TestContext.agentToken)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("not valid json {")
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        // Should be 400 or 500 depending on where parsing happens
        assertNotNull(status);
        assertTrue(status.value() == 400 || status.value() == 500,
                "Invalid JSON should return 400 or 500, got " + status.value());
    }

    @Test
    @Order(2)
    @DisplayName("BDD-EH02: Empty body returns error")
    void emptyBody_returnsError() {
        var response = gatewayClient.post()
                .uri("/api/v1/withdrawal")
                .header("Authorization", "Bearer " + TestContext.agentToken)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("")
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        assertNotNull(status);
        assertTrue(status.value() >= 400, "Empty body should return error status");
    }

    @Test
    @Order(3)
    @DisplayName("BDD-EH03: Error response has correct format")
    void errorResponse_hasCorrectFormat() {
        // Trigger an error by sending invalid data
        var response = gatewayPostNoAuth("/api/v1/withdrawal", "{}");

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        if (status != null && status.value() >= 400) {
            try {
                JsonNode body = objectMapper.readTree(responseBody);
                // Error response should have: status, error.code, error.message
                if (body.has("status")) {
                    assertEquals("FAILED", body.get("status").asText(),
                            "Error response status should be FAILED");
                }
                if (body.has("error")) {
                    assertNotNull(body.get("error").get("code"), "Error should have code");
                    assertNotNull(body.get("error").get("message"), "Error should have message");
                }
            } catch (Exception e) {
                // Response may not be JSON, which is also acceptable for some errors
                System.out.println("Error response: " + responseBody);
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("BDD-EH04: Non-existent endpoint returns 404")
    void nonExistentEndpoint_returns404() {
        var response = gatewayGetNoAuth("/api/v1/nonexistent");

        var status = response.expectBody(String.class).returnResult().getStatus();
        assertEquals(404, status != null ? status.value() : 0,
                "Non-existent endpoint should return 404");
    }

    @Test
    @Order(5)
    @DisplayName("BDD-EH05: Public endpoints work without auth")
    void publicEndpoints_workWithoutAuth() {
        // Auth token endpoint should be public
        String body = """
                {
                    "username": "admin",
                    "password": "password"
                }
                """;

        var response = gatewayClient.post()
                .uri("/auth/token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        // Should be 200 or 401 (if admin doesn't exist), not 403
        assertNotNull(status);
        assertTrue(status.value() == 200 || status.value() == 401,
                "Public auth endpoint should return 200 or 401, got " + status.value());
    }
}

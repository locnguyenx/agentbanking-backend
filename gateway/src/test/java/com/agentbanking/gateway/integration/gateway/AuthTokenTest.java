package com.agentbanking.gateway.integration.gateway;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 8b: Gateway Auth Token Tests
 * 
 * Tests auth token endpoints through the gateway (/api/v1/auth/*).
 * Covers token generation, refresh, and revocation flows.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(51)
class AuthTokenTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-AUTH01: POST /api/v1/auth/token returns 200 with valid credentials")
    void authToken_withValidCredentials_returns200() {
        String requestBody = """
                {
                    "username": "admin",
                    "password": "password"
                }
                """;

        String body = gatewayClient.post()
                .uri("/api/v1/auth/token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(body, "Response body should not be null");
        
        JsonNode node;
        try {
            node = objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token response", e);
        }
        
        assertNotNull(node.get("access_token"), "Response should contain access_token");
        assertNotNull(node.get("refresh_token"), "Response should contain refresh_token");
        assertNotNull(node.get("expires_in"), "Response should contain expires_in");
        assertEquals("Bearer", node.get("token_type").asText(), "Token type should be Bearer");
    }

    @Test
    @Order(2)
    @DisplayName("BDD-AUTH02: POST /api/v1/auth/token returns 400 with invalid password")
    void authToken_withInvalidPassword_returns400() {
        String requestBody = """
                {
                    "username": "admin",
                    "password": "wrongpassword"
                }
                """;

        gatewayClient.post()
                .uri("/api/v1/auth/token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(3)
    @DisplayName("BDD-AUTH03: POST /api/v1/auth/token returns 400 with non-existent user")
    void authToken_withNonExistentUser_returns400() {
        String requestBody = """
                {
                    "username": "nonexistentuser",
                    "password": "anypassword"
                }
                """;

        gatewayClient.post()
                .uri("/api/v1/auth/token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(4)
    @DisplayName("BDD-AUTH04: POST /api/v1/auth/token returns 400 with missing username")
    void authToken_withMissingUsername_returns400() {
        String requestBody = """
                {
                    "password": "password"
                }
                """;

        gatewayClient.post()
                .uri("/api/v1/auth/token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(5)
    @DisplayName("BDD-AUTH05: POST /api/v1/auth/token returns 400 with missing password")
    void authToken_withMissingPassword_returns400() {
        String requestBody = """
                {
                    "username": "admin"
                }
                """;

        gatewayClient.post()
                .uri("/api/v1/auth/token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(10)
    @DisplayName("BDD-AUTH10: POST /api/v1/auth/refresh returns 200 with valid refresh token")
    void authRefresh_withValidRefreshToken_returns200() {
        // First get tokens to get a valid refresh token
        String tokenBody = """
                {
                    "username": "admin",
                    "password": "password"
                }
                """;
        
        String tokenResponseBody = gatewayClient.post()
                .uri("/api/v1/auth/token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(tokenBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        
        JsonNode tokenNode;
        try {
            tokenNode = objectMapper.readTree(tokenResponseBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token response", e);
        }
        String refreshToken = tokenNode.get("refresh_token").asText();

        String requestBody = """
                {
                    "refresh_token": "%s"
                }
                """.formatted(refreshToken);

        String responseBody = gatewayClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        JsonNode node;
        try {
            node = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse refresh response", e);
        }
        assertNotNull(node.get("access_token"), "Response should contain new access_token");
        assertNotNull(node.get("refresh_token"), "Response should contain new refresh_token");
    }

    @Test
    @Order(11)
    @DisplayName("BDD-AUTH11: POST /api/v1/auth/refresh returns 401 with invalid refresh token")
    void authRefresh_withInvalidRefreshToken_returns401() {
        String requestBody = """
                {
                    "refresh_token": "invalid-refresh-token"
                }
                """;

        gatewayClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(12)
    @DisplayName("BDD-AUTH12: POST /api/v1/auth/refresh returns 400 with missing refresh_token")
    void authRefresh_withMissingRefreshToken_returns400() {
        String requestBody = """
                {}
                """;

        gatewayClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(20)
    @DisplayName("BDD-AUTH20: POST /api/v1/auth/revoke returns 204 with valid token")
    void authRevoke_withValidToken_returns204() {
        // Get a valid token first
        String adminToken = getToken("admin", "password");

        gatewayClient.post()
                .uri("/api/v1/auth/revoke")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @Order(21)
    @DisplayName("BDD-AUTH21: POST /api/v1/auth/revoke returns 401 without token")
    void authRevoke_withoutToken_returns401() {
        gatewayClient.post()
                .uri("/api/v1/auth/revoke")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Order(22)
    @DisplayName("BDD-AUTH22: POST /api/v1/auth/revoke returns 401 with invalid token")
    void authRevoke_withInvalidToken_returns401() {
        gatewayClient.post()
                .uri("/api/v1/auth/revoke")
                .header("Authorization", "Bearer invalid-token")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}

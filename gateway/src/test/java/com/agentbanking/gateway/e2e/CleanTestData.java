package com.agentbanking.gateway.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Utility class for cleaning up E2E test data.
 * 
 * Run with: ./gradlew :gateway:cleanE2eTestData
 * 
 * This class:
 * 1. Deletes test users from auth service
 * 2. Deactivates test agents from onboarding service
 * 3. Can be run before E2E tests to ensure clean state
 */
public class CleanTestData {

    private static final String GATEWAY_URL = System.getenv().getOrDefault(
            "GATEWAY_BASE_URL", "http://localhost:8080");
    private static final String AUTH_URL = System.getenv().getOrDefault(
            "AUTH_SERVICE_URL", "http://localhost:8087");
    private static final String ONBOARDING_URL = System.getenv().getOrDefault(
            "ONBOARDING_SERVICE_URL", "http://localhost:8083");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Test user patterns to clean up
    private static final String[] TEST_USER_PATTERNS = {
            "agent001", "operator001", "auditor001", "teller001",
            "maker001", "checker001", "compliance001", "supervisor001",
            "AGT-E2E-"
    };

    // Test agent patterns to clean up
    private static final String[] TEST_AGENT_PATTERNS = {
            "AGT-E2E-", "AGT-001", "AGT-002", "AGT-003"
    };

    public static void main(String[] args) {
        System.out.println("=== E2E Test Data Cleanup ===");
        System.out.println("Gateway URL: " + GATEWAY_URL);
        System.out.println("Auth URL: " + AUTH_URL);
        System.out.println("Onboarding URL: " + ONBOARDING_URL);
        System.out.println();

        try {
            // Step 1: Get admin token
            String adminToken = getAdminToken();
            if (adminToken == null) {
                System.out.println("WARNING: Could not get admin token. Trying bootstrap...");
                bootstrapAdmin();
                adminToken = getAdminToken();
            }

            if (adminToken == null) {
                System.out.println("ERROR: Cannot proceed without admin token");
                return;
            }

            System.out.println("Admin token obtained successfully");

            // Step 2: Clean up test users
            cleanTestUsers(adminToken);

            // Step 3: Clean up test agents
            cleanTestAgents(adminToken);

            System.out.println("\n=== Cleanup Complete ===");

        } catch (Exception e) {
            System.err.println("Cleanup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getAdminToken() {
        int maxRetries = 10;
        int waitMs = 3000;
        for (int i = 0; i < maxRetries; i++) {
            try {
                WebTestClient client = WebTestClient.bindToServer().baseUrl(AUTH_URL)
                        .responseTimeout(java.time.Duration.ofSeconds(30))
                        .build();
                String body = """
                        {
                            "username": "admin",
                            "password": "password"
                        }
                        """;

                String response = client.post()
                        .uri("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .exchange()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();

                JsonNode node = objectMapper.readTree(response);
                if (node.has("access_token")) {
                    return node.get("access_token").asText();
                }
            } catch (Exception e) {
                System.out.println("Wait for auth-service... (" + (i + 1) + ")");
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private static void bootstrapAdmin() {
        try {
            WebTestClient client = WebTestClient.bindToServer().baseUrl(AUTH_URL)
                    .responseTimeout(java.time.Duration.ofSeconds(30))
                    .build();
            String body = """
                    {
                        "username": "admin",
                        "email": "admin@agentbanking.com",
                        "password": "password",
                        "fullName": "System Administrator"
                    }
                    """;

            client.post()
                    .uri("/auth/users/bootstrap")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange()
                    .expectBody(String.class);

            System.out.println("Admin bootstrapped");
        } catch (Exception e) {
            System.out.println("Bootstrap may have failed (admin may already exist): " + e.getMessage());
        }
    }

    private static void cleanTestUsers(String adminToken) {
        System.out.println("\n--- Cleaning test users ---");

        try {
            WebTestClient client = WebTestClient.bindToServer().baseUrl(AUTH_URL)
                    .responseTimeout(java.time.Duration.ofSeconds(30))
                    .build();

            // List all users
            String response = client.get()
                    .uri("/auth/users")
                    .header("Authorization", "Bearer " + adminToken)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            JsonNode users = objectMapper.readTree(response);
            if (users.isArray()) {
                for (JsonNode user : users) {
                    String username = user.get("username").asText();
                    String userId = user.get("userId").asText();

                    // Check if this is a test user
                    boolean isTestUser = false;
                    for (String pattern : TEST_USER_PATTERNS) {
                        if (username.startsWith(pattern)) {
                            isTestUser = true;
                            break;
                        }
                    }

                    if (isTestUser) {
                        System.out.println("  Deleting user: " + username + " (" + userId + ")");
                        try {
                            client.delete()
                                    .uri("/auth/users/" + userId)
                                    .header("Authorization", "Bearer " + adminToken)
                                    .exchange();
                            System.out.println("    Deleted successfully");
                        } catch (Exception e) {
                            System.out.println("    Delete failed: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to clean users: " + e.getMessage());
        }
    }

    private static void cleanTestAgents(String adminToken) {
        System.out.println("\n--- Cleaning test agents ---");

        try {
            WebTestClient client = WebTestClient.bindToServer().baseUrl(ONBOARDING_URL)
                    .responseTimeout(java.time.Duration.ofSeconds(30))
                    .build();

            // List all agents
            String response = client.get()
                    .uri("/backoffice/agents?page=0&size=100")
                    .header("Authorization", "Bearer " + adminToken)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            JsonNode agents = objectMapper.readTree(response);
            JsonNode content = agents.has("content") ? agents.get("content") : agents;

            if (content.isArray()) {
                for (JsonNode agent : content) {
                    String agentCode = agent.has("agentCode") ? agent.get("agentCode").asText() : "";
                    String agentId = agent.get("agentId").asText();

                    // Check if this is a test agent
                    boolean isTestAgent = false;
                    for (String pattern : TEST_AGENT_PATTERNS) {
                        if (agentCode.startsWith(pattern)) {
                            isTestAgent = true;
                            break;
                        }
                    }

                    if (isTestAgent) {
                        System.out.println("  Deleting agent: " + agentCode + " (" + agentId + ")");
                        try {
                            client.delete()
                                    .uri("/internal/onboarding/agents/" + agentId)
                                    .header("Authorization", "Bearer " + adminToken)
                                    .exchange();
                            System.out.println("    Deleted successfully");
                        } catch (Exception e) {
                            System.out.println("    Deactivate failed: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to clean agents: " + e.getMessage());
        }
    }
}

package com.agentbanking.gateway.integration;

import com.agentbanking.gateway.integration.setup.TestContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Base class for E2E integration tests.
 * Uses real HTTP calls to running Docker services.
 * 
 * All tests extend this class for consistent infrastructure.
 */
public abstract class BaseIntegrationTest {

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    // Gateway client
    protected final WebTestClient gatewayClient = WebTestClient.bindToServer()
            .baseUrl(TestContext.GATEWAY_URL)
            .build();

    // Direct service clients (for setup/verification)
    protected final WebTestClient authClient = WebTestClient.bindToServer()
            .baseUrl(getServiceUrl("AUTH_SERVICE_URL", "http://localhost:8087"))
            .build();

    protected final WebTestClient ledgerClient = WebTestClient.bindToServer()
            .baseUrl(getServiceUrl("LEDGER_SERVICE_URL", "http://localhost:8082"))
            .build();

    protected final WebTestClient onboardingClient = WebTestClient.bindToServer()
            .baseUrl(getServiceUrl("ONBOARDING_SERVICE_URL", "http://localhost:8083"))
            .build();

    protected final WebTestClient rulesClient = WebTestClient.bindToServer()
            .baseUrl(getServiceUrl("RULES_SERVICE_URL", "http://localhost:8081"))
            .build();

    // Common test data constants (backward compatibility)
    protected static final String POS_TERMINAL_ID = "POS-001";
    protected static final String GPS_LAT = "3.1390";
    protected static final String GPS_LNG = "101.6869";
    protected static final String MYR_CURRENCY = "MYR";
    protected static final String ENCRYPTED_CARD = "encrypted_card_blob";
    protected static final String DUKPT_PIN = "dukpt_encrypted_pin_block";
    protected static final String VALID_MYKAD = "123456789012";
    protected static final String INVALID_MYKAD = "000000000000";

    @BeforeAll
    static void checkServicesAvailable() {
        // Check that at least the gateway is running
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl(TestContext.GATEWAY_URL)
                .build();
        try {
            client.get().uri("/actuator/health")
                    .exchange()
                    .expectStatus().isOk();
        } catch (Exception e) {
            System.out.println("WARNING: Gateway not available at " + TestContext.GATEWAY_URL);
            System.out.println("Start Docker services with: docker-compose up -d");
        }
    }

    // ================================================================
    // AUTH HELPERS (call auth service directly for setup)
    // ================================================================

    /**
     * Bootstrap the admin user (first-time setup).
     * Only works when no users exist yet.
     */
    protected WebTestClient.ResponseSpec bootstrapAdmin() {
        String body = """
                {
                    "username": "%s",
                    "email": "admin@agentbanking.com",
                    "password": "%s",
                    "fullName": "System Administrator"
                }
                """.formatted(TestContext.ADMIN_USERNAME, TestContext.ADMIN_PASSWORD);

        return authClient.post()
                .uri("/auth/users/bootstrap")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    /**
     * Create a user via the auth service.
     */
    protected WebTestClient.ResponseSpec createUser(String username, String email, 
                                                      String password, String fullName, 
                                                      String token) {
        String body = """
                {
                    "username": "%s",
                    "email": "%s",
                    "password": "%s",
                    "fullName": "%s"
                }
                """.formatted(username, email, password, fullName);

        return authClient.post()
                .uri("/auth/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    /**
     * Get a JWT token from the auth service.
     */
    protected String getToken(String username, String password) {
        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        String response = authClient.post()
                .uri("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        try {
            JsonNode node = objectMapper.readTree(response);
            return node.get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token response", e);
        }
    }

    /**
     * Get user details by ID.
     */
    protected JsonNode getUser(String userId, String token) {
        String response = authClient.get()
                .uri("/auth/users/" + userId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse user response", e);
        }
    }

    // ================================================================
    // ONBOARDING HELPERS
    // ================================================================

    /**
     * Create an agent via the onboarding service.
     */
    protected WebTestClient.ResponseSpec createAgent(String agentCode, String businessName,
                                                       String tier, String mykadNumber,
                                                       String phoneNumber, double gpsLat,
                                                       double gpsLng, String token) {
        String body = """
                {
                    "agentCode": "%s",
                    "businessName": "%s",
                    "tier": "%s",
                    "mykadNumber": "%s",
                    "phoneNumber": "%s",
                    "merchantGpsLat": %s,
                    "merchantGpsLng": %s
                }
                """.formatted(agentCode, businessName, tier, mykadNumber, phoneNumber,
                        gpsLat, gpsLng);

        return onboardingClient.post()
                .uri("/backoffice/agents")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    /**
     * Get agent details by ID.
     */
    protected WebTestClient.ResponseSpec getAgent(UUID agentId, String token) {
        return onboardingClient.get()
                .uri("/backoffice/agents/" + agentId)
                .header("Authorization", "Bearer " + token)
                .exchange();
    }

    /**
     * List all agents.
     */
    protected WebTestClient.ResponseSpec listAgents(String token) {
        return onboardingClient.get()
                .uri("/backoffice/agents?page=0&size=50")
                .header("Authorization", "Bearer " + token)
                .exchange();
    }

    // ================================================================
    // LEDGER HELPERS
    // ================================================================

    /**
     * Get agent float balance.
     */
    protected WebTestClient.ResponseSpec getBalance(UUID agentId, String token) {
        return ledgerClient.get()
                .uri("/internal/balance/" + agentId)
                .exchange();
    }

    /**
     * Make a deposit to increase float balance.
     */
    protected WebTestClient.ResponseSpec deposit(UUID agentId, double amount, 
                                                    String idempotencyKey) {
        String body = """
                {
                    "agentId": "%s",
                    "amount": %s,
                    "customerFee": null,
                    "agentCommission": null,
                    "bankShare": null,
                    "idempotencyKey": "%s",
                    "destinationAccount": "DEPOSIT_ACCOUNT"
                }
                """.formatted(agentId, amount, idempotencyKey);

        return ledgerClient.post()
                .uri("/internal/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    /**
     * Make a withdrawal.
     */
    protected WebTestClient.ResponseSpec withdraw(UUID agentId, double amount,
                                                     String idempotencyKey) {
        String body = """
                {
                    "agentId": "%s",
                    "amount": %s,
                    "customerFee": null,
                    "agentCommission": null,
                    "bankShare": null,
                    "idempotencyKey": "%s",
                    "customerCardMasked": "411111******1111",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(agentId, amount, idempotencyKey);

        return ledgerClient.post()
                .uri("/internal/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    // ================================================================
    // GATEWAY REQUEST BUILDERS
    // ================================================================

    protected String idempotencyKey() {
        return UUID.randomUUID().toString();
    }

    protected WebTestClient.ResponseSpec gatewayPost(String uri, String token, String body) {
        return gatewayClient.post()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .header("X-Idempotency-Key", idempotencyKey())
                .header("X-POS-Terminal-Id", TestContext.POS_TERMINAL_ID)
                .header("X-GPS-Latitude", TestContext.GPS_LAT)
                .header("X-GPS-Longitude", TestContext.GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    protected WebTestClient.ResponseSpec gatewayGet(String uri, String token) {
        return gatewayClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .exchange();
    }

    protected WebTestClient.ResponseSpec gatewayPut(String uri, String token, String body) {
        return gatewayClient.put()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    protected WebTestClient.ResponseSpec gatewayDelete(String uri, String token) {
        return gatewayClient.delete()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .exchange();
    }

    protected WebTestClient.ResponseSpec gatewayPostNoAuth(String uri, String body) {
        return gatewayClient.post()
                .uri(uri)
                .header("X-Idempotency-Key", idempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }

    protected WebTestClient.ResponseSpec gatewayGetNoAuth(String uri) {
        return gatewayClient.get()
                .uri(uri)
                .exchange();
    }

    // ================================================================
    // UTILITY
    // ================================================================

    private static String getServiceUrl(String envKey, String defaultValue) {
        return System.getenv().getOrDefault(envKey, defaultValue);
    }

    /**
     * Extract a JSON body and parse it.
     */
    protected JsonNode parseBody(WebTestClient.ResponseSpec response) {
        String body = response.expectBody(String.class).returnResult().getResponseBody();
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response body: " + body, e);
        }
    }

    // ================================================================
    // BACKWARD COMPATIBILITY (for existing tests like LedgerFloatIntegrationTest)
    // ================================================================

    // Old agent ID constants (deprecated - use TestContext.agentId)
    @Deprecated
    protected static final String MICRO_AGENT_ID = getEnvOrDefault("TEST_MICRO_AGENT_ID", "AGT-01");
    @Deprecated
    protected static final String STANDARD_AGENT_ID = getEnvOrDefault("TEST_STANDARD_AGENT_ID", "AGT-03");
    @Deprecated
    protected static final String PREMIER_AGENT_ID = getEnvOrDefault("TEST_PREMIER_AGENT_ID", "AGT-02");

    // Old token methods (deprecated - use TestContext tokens)
    @Deprecated
    protected String getMicroAgentToken() {
        return getToken("agent001", "AgentPass123!");
    }

    @Deprecated
    protected String getStandardAgentToken() {
        return getToken("agent001", "AgentPass123!");
    }

    @Deprecated
    protected String getPremierAgentToken() {
        return getToken("agent001", "AgentPass123!");
    }

    @Deprecated
    protected String getOperatorToken() {
        return getToken("operator001", "OperatorPass123!");
    }

    @Deprecated
    protected String getAdminToken() {
        return getToken("admin", "password");
    }

    @Deprecated
    protected String getComplianceOfficerToken() {
        return getToken("compliance001", "CompliancePass123!");
    }

    @Deprecated
    protected String getMakerToken() {
        return getToken("maker001", "MakerPass123!");
    }

    @Deprecated
    protected String getCheckerToken() {
        return getToken("checker001", "CheckerPass123!");
    }

    @Deprecated
    protected String getSupervisorToken() {
        return getToken("supervisor001", "SupervisorPass123!");
    }

    // Old request methods (deprecated - use gatewayPost, gatewayGet)
    @Deprecated
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPost(String uri, String token, String requestBody) {
        return gatewayClient.post()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .header("X-Idempotency-Key", idempotencyKey())
                .header("X-POS-Terminal-Id", TestContext.POS_TERMINAL_ID)
                .header("X-GPS-Latitude", TestContext.GPS_LAT)
                .header("X-GPS-Longitude", TestContext.GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody);
    }

    @Deprecated
    protected WebTestClient.RequestHeadersSpec<?> authenticatedGet(String uri, String token) {
        return gatewayClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + token);
    }

    @Deprecated
    protected WebTestClient.RequestHeadersSpec<?> authenticatedPut(String uri, String token, String requestBody) {
        return gatewayClient.put()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody);
    }

    @Deprecated
    protected WebTestClient.RequestHeadersSpec<?> authenticatedDelete(String uri, String token) {
        return gatewayClient.delete()
                .uri(uri)
                .header("Authorization", "Bearer " + token);
    }

    @Deprecated
    protected WebTestClient.RequestHeadersSpec<?> noAuthPost(String uri, String requestBody) {
        return gatewayClient.post()
                .uri(uri)
                .header("X-Idempotency-Key", idempotencyKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody);
    }

    @Deprecated
    protected WebTestClient.RequestHeadersSpec<?> noAuthGet(String uri) {
        return gatewayClient.get().uri(uri);
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}

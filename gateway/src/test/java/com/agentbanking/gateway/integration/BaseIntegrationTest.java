package com.agentbanking.gateway.integration;

import com.agentbanking.gateway.util.AuthTokenProvider;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Base class for BDD-aligned integration tests.
 * Uses real JWT tokens from the auth system for E2E testing.
 * 
 * Configure auth credentials in application-test.yaml or via environment variables:
 * - AUTH_TOKEN_ENDPOINT: URL to auth service
 * - TEST_AGENT_CLIENT_ID, TEST_AGENT_CLIENT_SECRET: Agent credentials
 * - TEST_OPERATOR_USERNAME, TEST_OPERATOR_PASSWORD: Operator credentials
 * - TEST_ADMIN_USERNAME, TEST_ADMIN_PASSWORD: Admin credentials
 */
public abstract class BaseIntegrationTest {

    private static final String GATEWAY_URL = System.getenv().getOrDefault(
            "GATEWAY_BASE_URL", "http://localhost:8080");

    protected final WebTestClient webTestClient = WebTestClient.bindToServer()
            .baseUrl(GATEWAY_URL)
            .build();

    // Common test data
    protected static final String POS_TERMINAL_ID = "POS-001";
    protected static final String GPS_LAT = "3.1390";
    protected static final String GPS_LNG = "101.6869";
    protected static final String MYR_CURRENCY = "MYR";
    protected static final String ENCRYPTED_CARD = "encrypted_card_blob";
    protected static final String DUKPT_PIN = "dukpt_encrypted_pin_block";
    protected static final String VALID_MYKAD = "123456789012";
    protected static final String INVALID_MYKAD = "000000000000";

    // Agent IDs
    protected static final String MICRO_AGENT_ID = getEnvOrDefault("TEST_MICRO_AGENT_ID", "AGT-01");
    protected static final String STANDARD_AGENT_ID = getEnvOrDefault("TEST_STANDARD_AGENT_ID", "AGT-03");
    protected static final String PREMIER_AGENT_ID = getEnvOrDefault("TEST_PREMIER_AGENT_ID", "AGT-02");

    // Auth service availability flag
    private static boolean authServiceAvailable = false;

    @BeforeAll
    static void checkAuthServiceAvailability() {
        authServiceAvailable = AuthTokenProvider.isAvailable();
        if (!authServiceAvailable) {
            System.out.println("WARNING: Auth service not available. Tests requiring auth will be skipped.");
            System.out.println("Set AUTH_TOKEN_ENDPOINT and credentials to enable full E2E tests.");
        }
    }

    protected static boolean isAuthServiceAvailable() {
        return authServiceAvailable;
    }

    protected static void assumeAuthServiceAvailable() {
        assumeTrue(authServiceAvailable,
            "Auth service not available. " +
            "Configure AUTH_TOKEN_ENDPOINT and credentials in application-test.yaml");
    }

    // Token helpers - use real auth service
    protected String getAgentToken(String agentId, String tier) {
        assumeAuthServiceAvailable();
        return AuthTokenProvider.getAgentToken(agentId, tier);
    }

    protected String getMicroAgentToken() {
        return getAgentToken(MICRO_AGENT_ID, "MICRO");
    }

    protected String getStandardAgentToken() {
        return getAgentToken(STANDARD_AGENT_ID, "STANDARD");
    }

    protected String getPremierAgentToken() {
        return getAgentToken(PREMIER_AGENT_ID, "PREMIER");
    }

    protected String getOperatorToken() {
        assumeAuthServiceAvailable();
        return AuthTokenProvider.getOperatorToken();
    }

    protected String getAdminToken() {
        assumeAuthServiceAvailable();
        return AuthTokenProvider.getAdminToken();
    }

    protected String getComplianceOfficerToken() {
        assumeAuthServiceAvailable();
        return AuthTokenProvider.getComplianceOfficerToken();
    }

    protected String getMakerToken() {
        assumeAuthServiceAvailable();
        return AuthTokenProvider.getMakerToken();
    }

    protected String getCheckerToken() {
        assumeAuthServiceAvailable();
        return AuthTokenProvider.getCheckerToken();
    }

    protected String getSupervisorToken() {
        assumeAuthServiceAvailable();
        return AuthTokenProvider.getSupervisorToken();
    }

    // Request builders
    protected String idempotencyKey() {
        return UUID.randomUUID().toString();
    }

    protected WebTestClient.RequestHeadersSpec<?> authenticatedPost(String uri, String token, String requestBody) {
        return webTestClient.post()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .header("X-Idempotency-Key", idempotencyKey())
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody);
    }

    protected WebTestClient.RequestHeadersSpec<?> authenticatedGet(String uri, String token) {
        return webTestClient.get()
                .uri(uri)
                .header("Authorization", "Bearer " + token);
    }

    protected WebTestClient.RequestHeadersSpec<?> authenticatedPut(String uri, String token, String requestBody) {
        return webTestClient.put()
                .uri(uri)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody);
    }

    protected WebTestClient.RequestHeadersSpec<?> authenticatedDelete(String uri, String token) {
        return webTestClient.delete()
                .uri(uri)
                .header("Authorization", "Bearer " + token);
    }

    protected WebTestClient.RequestHeadersSpec<?> noAuthPost(String uri, String requestBody) {
        return webTestClient.post()
                .uri(uri)
                .header("X-Idempotency-Key", idempotencyKey())
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody);
    }

    protected WebTestClient.RequestHeadersSpec<?> noAuthGet(String uri) {
        return webTestClient.get().uri(uri);
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}

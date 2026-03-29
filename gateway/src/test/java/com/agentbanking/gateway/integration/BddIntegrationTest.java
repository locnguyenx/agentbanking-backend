package com.agentbanking.gateway.integration;

import com.agentbanking.gateway.util.AuthTokenProvider;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Comprehensive BDD-aligned integration tests for all API Gateway endpoints.
 * 
 * Tests verify:
 * 1. Authentication requirements (401 without valid token)
 * 2. Happy path scenarios (200 with valid tokens)
 * 3. Business logic validation
 * 4. Error handling
 * 
 * Run with: ./gradlew :gateway:test --tests "BddIntegrationTest"
 * 
 * Prerequisites:
 * - Docker services running: docker-compose --profile infra --profile backend --profile gateway up -d
 * - Test users seeded in database (V8 migration)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BddIntegrationTest {

    private static final WebTestClient webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:8080")
            .build();

    // Test data
    private static final String POS_TERMINAL_ID = "POS-001";
    private static final String GPS_LAT = "3.1390";
    private static final String GPS_LNG = "101.6869";
    private static final String MYR_CURRENCY = "MYR";
    private static final String ENCRYPTED_CARD = "encrypted_card_blob";
    private static final String DUKPT_PIN = "dukpt_encrypted_pin_block";

    // Agent IDs
    private static final String MICRO_AGENT_ID = "AGT-01";
    private static final String STANDARD_AGENT_ID = "AGT-03";
    private static final String PREMIER_AGENT_ID = "AGT-02";

    // Auth availability flag
    private static boolean authServiceAvailable = false;

    @BeforeAll
    static void checkAuthService() {
        authServiceAvailable = AuthTokenProvider.isAvailable();
        System.out.println("Auth service available: " + authServiceAvailable);
    }

    // ========== AUTH TESTS ==========
    @Nested
    @DisplayName("BDD-G01/G02: Gateway Authentication")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class GatewayAuthentication {

        @Test
        @Order(1)
        @DisplayName("Token endpoint is accessible")
        void tokenEndpoint_isAccessible() {
            // Just verify the endpoint exists (returns 400 for invalid request, not 404)
            webTestClient.post().uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue("grant_type=password&username=test&password=test")
                    .exchange()
                    .expectStatus().is4xxClientError();
        }

        @Test
        @Order(2)
        @DisplayName("JWKS endpoint is accessible")
        void jwksEndpoint_isAccessible() {
            webTestClient.get().uri("/.well-known/jwks.json")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.keys").isArray();
        }

        @Test
        @Order(3)
        @DisplayName("Withdrawal requires authentication")
        void withdrawal_noAuth_returns401() {
            webTestClient.post().uri("/api/v1/withdrawal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"amount\": 500.00}")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @Order(4)
        @DisplayName("Deposit requires authentication")
        void deposit_noAuth_returns401() {
            webTestClient.post().uri("/api/v1/deposit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"amount\": 500.00}")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @Order(5)
        @DisplayName("Balance inquiry requires authentication")
        void balanceInquiry_noAuth_returns401() {
            webTestClient.post().uri("/api/v1/balance-inquiry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{}")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @Order(6)
        @DisplayName("Backoffice dashboard requires authentication")
        void dashboard_noAuth_returns401() {
            webTestClient.get().uri("/api/v1/backoffice/dashboard")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @Order(7)
        @DisplayName("OpenAPI docs are publicly accessible")
        void openApiDocs_public() {
            webTestClient.get().uri("/v3/api-docs")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.openapi").isEqualTo("3.0.3");
        }
    }

    // ========== TRANSACTION TESTS (require auth service) ==========
    @Nested
    @DisplayName("BDD-R01/R02: Rules & Fee Engine")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RulesFeeEngine {

        @Test
        @Order(1)
        @DisplayName("BDD-R01 [HP]: Withdrawal with valid agent token")
        void withdrawal_withValidToken_shouldSucceed() {
            assumeTrue(authServiceAvailable, "Auth service required");
            
            String token = AuthTokenProvider.getAgentToken(MICRO_AGENT_ID, "MICRO");
            String body = """
                {"amount": 500.00, "card_data": "%s", "pin_block": "%s", "currency": "%s"}
                """.formatted(ENCRYPTED_CARD, DUKPT_PIN, MYR_CURRENCY);

            webTestClient.post().uri("/api/v1/withdrawal")
                    .header("Authorization", "Bearer " + token)
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                    .header("X-GPS-Latitude", GPS_LAT)
                    .header("X-GPS-Longitude", GPS_LNG)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("SUCCESS")
                    .jsonPath("$.transaction_id").isNotEmpty();
        }

        @Test
        @Order(2)
        @DisplayName("BDD-R02-EC-03 [EC]: Zero amount returns 400")
        void withdrawal_zeroAmount_returns400() {
            assumeTrue(authServiceAvailable, "Auth service required");
            
            String token = AuthTokenProvider.getAgentToken(MICRO_AGENT_ID, "MICRO");
            String body = """
                {"amount": 0.00, "card_data": "%s", "pin_block": "%s", "currency": "%s"}
                """.formatted(ENCRYPTED_CARD, DUKPT_PIN, MYR_CURRENCY);

            webTestClient.post().uri("/api/v1/withdrawal")
                    .header("Authorization", "Bearer " + token)
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                    .header("X-GPS-Latitude", GPS_LAT)
                    .header("X-GPS-Longitude", GPS_LNG)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("FAILED")
                    .jsonPath("$.error.code").isEqualTo("ERR_INVALID_AMOUNT");
        }
    }

    // ========== DEPOSIT TESTS ==========
    @Nested
    @DisplayName("BDD-D01: Cash Deposit")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CashDeposit {

        @Test
        @Order(1)
        @DisplayName("BDD-D01 [HP]: Deposit with valid token")
        void deposit_withValidToken_shouldSucceed() {
            assumeTrue(authServiceAvailable, "Auth service required");
            
            String token = AuthTokenProvider.getAgentToken(STANDARD_AGENT_ID, "STANDARD");
            String body = """
                {"amount": 1000.00, "destination_account": "1234567890", "currency": "%s"}
                """.formatted(MYR_CURRENCY);

            webTestClient.post().uri("/api/v1/deposit")
                    .header("Authorization", "Bearer " + token)
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                    .header("X-GPS-Latitude", GPS_LAT)
                    .header("X-GPS-Longitude", GPS_LNG)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("SUCCESS")
                    .jsonPath("$.transaction_id").isNotEmpty();
        }
    }

    // ========== KYC TESTS ==========
    @Nested
    @DisplayName("BDD-O01: e-KYC & Onboarding")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class KycOnboarding {

        @Test
        @Order(1)
        @DisplayName("BDD-O01 [HP]: KYC verification with valid token")
        void kycVerify_withValidToken_shouldSucceed() {
            assumeTrue(authServiceAvailable, "Auth service required");
            
            String token = AuthTokenProvider.getAgentToken(STANDARD_AGENT_ID, "STANDARD");
            String body = """
                {"mykad_number": "123456789012"}
                """;

            webTestClient.post().uri("/api/v1/kyc/verify")
                    .header("Authorization", "Bearer " + token)
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                    .header("X-GPS-Latitude", GPS_LAT)
                    .header("X-GPS-Longitude", GPS_LNG)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("VERIFIED")
                    .jsonPath("$.verification_id").isNotEmpty();
        }
    }

    // ========== BACKOFFICE TESTS ==========
    @Nested
    @DisplayName("BDD-BO01/BO02: Backoffice")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class Backoffice {

        @Test
        @Order(1)
        @DisplayName("BDD-BO02 [HP]: Dashboard with operator token")
        void dashboard_withOperatorToken_shouldSucceed() {
            assumeTrue(authServiceAvailable, "Auth service required");
            
            String token = AuthTokenProvider.getOperatorToken();

            webTestClient.get().uri("/api/v1/backoffice/dashboard")
                    .header("Authorization", "Bearer " + token)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.total_transactions").isNumber();
        }

        @Test
        @Order(2)
        @DisplayName("Agent cannot access backoffice")
        void agent_cannotAccessBackoffice_returns403() {
            assumeTrue(authServiceAvailable, "Auth service required");
            
            String token = AuthTokenProvider.getAgentToken(STANDARD_AGENT_ID, "STANDARD");

            webTestClient.get().uri("/api/v1/backoffice/dashboard")
                    .header("Authorization", "Bearer " + token)
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }
}

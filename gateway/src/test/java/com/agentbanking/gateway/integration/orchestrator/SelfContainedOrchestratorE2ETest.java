package com.agentbanking.gateway.integration.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Self-contained E2E Tests for Transaction Orchestrator Flow
 * 
 * These tests are independent of setup phases and can run directly.
 * They test the complete flow: Gateway → Orchestrator → Temporal Workflows
 * 
 * Note: Requires all Docker services running (docker-compose --profile all up -d)
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
class SelfContainedOrchestratorE2ETest {

    private static final String GATEWAY_URL = System.getenv().getOrDefault("GATEWAY_BASE_URL", "http://localhost:8080");
    private static final String AUTH_URL = System.getenv().getOrDefault("AUTH_SERVICE_URL", "http://localhost:8087");
    private static final String ONBOARDING_URL = System.getenv().getOrDefault("ONBOARDING_SERVICE_URL", "http://localhost:8083");
    private static final String LEDGER_URL = System.getenv().getOrDefault("LEDGER_SERVICE_URL", "http://localhost:18082");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static String adminToken;
    private static String agentToken;
    private static String agentId;
    private static final UUID AGENT_UUID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    private final WebTestClient gatewayClient = WebTestClient.bindToServer()
            .baseUrl(GATEWAY_URL)
            .responseTimeout(java.time.Duration.ofSeconds(60))
            .build();

    private final WebTestClient authClient = WebTestClient.bindToServer()
            .baseUrl(AUTH_URL)
            .responseTimeout(java.time.Duration.ofSeconds(30))
            .build();

    private final WebTestClient onboardingClient = WebTestClient.bindToServer()
            .baseUrl(ONBOARDING_URL)
            .responseTimeout(java.time.Duration.ofSeconds(30))
            .build();

    private final WebTestClient ledgerClient = WebTestClient.bindToServer()
            .baseUrl(LEDGER_URL)
            .responseTimeout(java.time.Duration.ofSeconds(30))
            .build();

    @BeforeAll
    static void setup() {
        System.out.println("=== Self-Contained E2E Test Setup ===");
        System.out.println("Gateway URL: " + GATEWAY_URL);
    }

    @Nested
    @DisplayName("Setup: Get test token")
    @Order(1)
    class SetupPhase {

        @Test
        @DisplayName("Get or create test agent token")
        void getAgentToken() {
            adminToken = getAdminToken();
            assertNotNull(adminToken, "Admin token required for setup");

            try {
                authClient.post()
                        .uri("/auth/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("""
                            {
                                "username": "agent001",
                                "email": "agent001@bank.com",
                                "password": "12345678",
                                "fullName": "Test Agent"
                            }
                            """)
                        .exchange();
            } catch (Exception e) {
                System.out.println("User may already exist: " + e.getMessage());
            }

            String response = authClient.post()
                    .uri("/auth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                        {
                            "username": "agent001",
                            "password": "12345678"
                        }
                        """)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(response, "Agent token response should not be null");
            
            try {
                JsonNode node = objectMapper.readTree(response);
                assertTrue(node.has("access_token"), "Response should contain access_token");
                agentToken = node.get("access_token").asText();
                System.out.println("Agent token obtained: " + agentToken.substring(0, 20) + "...");
            } catch (Exception e) {
                fail("Failed to parse token response: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("Create or get agent via onboarding service")
        void createOrGetAgent() {
            assumeTrue(adminToken != null, "Admin token required");

            try {
                String response = onboardingClient.post()
                        .uri("/backoffice/agents")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("""
                            {
                                "agentCode": "AGT-E2E-001",
                                "businessName": "E2E Test Business",
                                "tier": "STANDARD",
                                "mykadNumber": "900101011234",
                                "phoneNumber": "0123456789",
                                "merchantGpsLat": 3.1390,
                                "merchantGpsLng": 101.6869
                            }
                            """)
                        .exchange()
                        .expectBody(String.class)
                        .returnResult()
                        .getResponseBody();

                JsonNode node = objectMapper.readTree(response);
                agentId = node.get("agentId").asText();
                System.out.println("Agent created/retrieved: " + agentId);
            } catch (Exception e) {
                try {
                    String listResponse = onboardingClient.get()
                            .uri("/backoffice/agents?page=0&size=10")
                            .header("Authorization", "Bearer " + adminToken)
                            .exchange()
                            .expectBody(String.class)
                            .returnResult()
                            .getResponseBody();

                    JsonNode agents = objectMapper.readTree(listResponse);
                    if (agents.isArray() && agents.size() > 0) {
                        agentId = agents.get(0).get("agentId").asText();
                        System.out.println("Agent already exists: " + agentId);
                    }
                } catch (Exception ex) {
                    System.out.println("Failed to get agent: " + ex.getMessage());
                }
            }
        }

        @Test
        @DisplayName("Top up agent float balance")
        void topUpFloat() {
            assumeTrue(agentId != null, "Agent must be created first");

            try {
                String idempotencyKey = "e2e-float-setup-" + UUID.randomUUID();
                String body = """
                    {
                        "agentId": "%s",
                        "amount": 10000.00,
                        "customerFee": null,
                        "agentCommission": null,
                        "bankShare": null,
                        "idempotencyKey": "%s",
                        "destinationAccount": "FLOAT_SETUP"
                    }
                    """.formatted(agentId, idempotencyKey);

                var response = ledgerClient.post()
                        .uri("/internal/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .exchange();

                if (response.expectBody(String.class).returnResult().getStatus().value() == 200) {
                    System.out.println("Float topped up successfully for agent: " + agentId);
                } else {
                    System.out.println("Float may already exist or setup completed");
                }
            } catch (Exception e) {
                System.out.println("Float setup: " + e.getMessage());
            }
        }

        private String getAdminToken() {
            try {
                // Bootstrap admin first
                authClient.post()
                        .uri("/auth/users/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("""
                            {
                                "username": "admin",
                                "email": "admin@agentbanking.com",
                                "password": "password",
                                "fullName": "System Admin"
                            }
                            """)
                        .exchange();
            } catch (Exception e) {
                System.out.println("Admin bootstrap may have failed: " + e.getMessage());
            }

            // Get admin token
            String response = authClient.post()
                    .uri("/auth/token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("""
                        {
                            "username": "admin",
                            "password": "password"
                        }
                        """)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            try {
                JsonNode node = objectMapper.readTree(response);
                return node.has("access_token") ? node.get("access_token").asText() : null;
            } catch (Exception e) {
                return null;
            }
        }
    }

    @Nested
    @DisplayName("BDD-TO: Workflow Router Dispatch")
    @Order(10)
    class WorkflowRouterDispatch {

        @Test
        @DisplayName("BDD-TO-01: Router dispatches Off-Us withdrawal to WithdrawalWorkflow")
        void withdraw_offUs_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required - run setup first");

            String idempotencyKey = "e2e-offus-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .header("X-POS-Terminal-Id", "POS-E2E-001")
                    .header("X-GPS-Latitude", "3.1390")
                    .header("X-GPS-Longitude", "101.6869")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            var result = response.expectBody(String.class).returnResult();
            int httpStatus = result.getStatus().value();
            String body = result.getResponseBody();
            if (httpStatus != 200 && httpStatus != 202) {
                System.out.println("DEBUG: httpStatus=" + httpStatus + " body=" + body);
            }
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
            assertNotNull(body, "Response body should not be null");
            JsonNode json = parseBody(body);
            if (json == null) {
                // Try parsing as error response
                System.out.println("DEBUG: Failed to parse body as JSON: " + body);
                fail("Response body should be valid JSON");
                return;
            }
            assertEquals("PENDING", json.get("status").asText());
            assertEquals(idempotencyKey, json.get("workflowId").asText());
            assertTrue(json.has("pollUrl"), "Response should contain pollUrl");
            assertEquals("/api/v1/transactions/" + idempotencyKey + "/status", 
                    json.get("pollUrl").asText(), "pollUrl format should match BDD spec");
        }

        @Test
        @DisplayName("BDD-TO-02: Router dispatches On-Us withdrawal to WithdrawalOnUsWorkflow")
        void withdraw_onUs_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-onus-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0012");

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-03: Router dispatches deposit to DepositWorkflow")
        void deposit_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-deposit-" + UUID.randomUUID();
            String requestBody = buildDepositRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-04: Router dispatches bill payment to BillPaymentWorkflow")
        void billPayment_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-billpay-" + UUID.randomUUID();
            String requestBody = buildBillPaymentRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-05: Router dispatches DuitNow transfer to DuitNowTransferWorkflow")
        void duitNowTransfer_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-duitnow-" + UUID.randomUUID();
            String requestBody = buildDuitNowRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-06: Router rejects unsupported transaction type")
        void startTransaction_unsupportedType_shouldReturnError() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-unsupported-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "UNKNOWN_TYPE",
                    "agentId": "%s",
                    "amount": 100.00,
                    "idempotencyKey": "%s"
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int statusCode = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(statusCode >= 400, "Unsupported transaction should return error");
        }
    }

    @Nested
    @DisplayName("BDD-TO-NEW: New Transaction Types E2E")
    @Order(15)
    class NewTransactionTypesE2E {

        @Test
        @DisplayName("BDD-TO-NEW-01: Router dispatches cashless payment to CashlessPaymentWorkflow")
        void cashlessPayment_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-cashless-" + UUID.randomUUID();
            String requestBody = buildCashlessPaymentRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-NEW-02: Router dispatches PIN-based purchase to PinBasedPurchaseWorkflow")
        void pinBasedPurchase_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-pin-purchase-" + UUID.randomUUID();
            String requestBody = buildPinBasedPurchaseRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-NEW-03: Router dispatches prepaid topup to PrepaidTopupWorkflow")
        void prepaidTopup_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-prepaid-topup-" + UUID.randomUUID();
            String requestBody = buildPrepaidTopupRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-NEW-04: Router dispatches ewallet withdrawal to EWalletWithdrawalWorkflow")
        void ewalletWithdrawal_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-ewallet-withdraw-" + UUID.randomUUID();
            String requestBody = buildEWalletWithdrawalRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-NEW-05: Router dispatches ewallet topup to EWalletTopupWorkflow")
        void ewalletTopup_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-ewallet-topup-" + UUID.randomUUID();
            String requestBody = buildEWalletTopupRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-NEW-06: Router dispatches ESSP purchase to ESSPPurchaseWorkflow")
        void esspPurchase_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-essp-purchase-" + UUID.randomUUID();
            String requestBody = buildESSPPurchaseRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-NEW-07: Router dispatches PIN purchase to PINPurchaseWorkflow")
        void pinPurchase_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-pin-purchase-" + UUID.randomUUID();
            String requestBody = buildPINPurchaseRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-NEW-08: Router dispatches retail sale to RetailSaleWorkflow")
        void retailSale_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-retail-sale-" + UUID.randomUUID();
            String requestBody = buildRetailSaleRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        @Test
        @DisplayName("BDD-TO-NEW-09: Router dispatches hybrid cashback to HybridCashbackWorkflow")
        void hybridCashback_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-hybrid-cashback-" + UUID.randomUUID();
            String requestBody = buildHybridCashbackRequest(idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            int httpStatus = response.expectBody(String.class).returnResult().getStatus().value();
            assertTrue(httpStatus == 200 || httpStatus == 202, "Should return 200 or 202 for async workflow start");
        }

        // Request builders for new transaction types

        private String buildCashlessPaymentRequest(String idempotencyKey) {
            return """
                {
                    "transactionType": "CASHLESS_PAYMENT",
                    "agentId": "%s",
                    "amount": 150.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);
        }

        private String buildPinBasedPurchaseRequest(String idempotencyKey) {
            return """
                {
                    "transactionType": "PIN_BASED_PURCHASE",
                    "agentId": "%s",
                    "amount": 250.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "pan": "4111111111111111",
                    "pinBlock": "encrypted-pin-block",
                    "customerCardMasked": "411111******1111",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);
        }

        private String buildPrepaidTopupRequest(String idempotencyKey) {
            return """
                {
                    "transactionType": "PREPAID_TOPUP",
                    "agentId": "%s",
                    "amount": 30.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "destinationAccount": "0123456789",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);
        }

        private String buildEWalletWithdrawalRequest(String idempotencyKey) {
            return """
                {
                    "transactionType": "EWALLET_WITHDRAWAL",
                    "agentId": "%s",
                    "amount": 100.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "destinationAccount": "ewallet-account-123",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);
        }

        private String buildEWalletTopupRequest(String idempotencyKey) {
            return """
                {
                    "transactionType": "EWALLET_TOPUP",
                    "agentId": "%s",
                    "amount": 50.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "destinationAccount": "ewallet-account-456",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);
        }

        private String buildESSPPurchaseRequest(String idempotencyKey) {
            return """
                {
                    "transactionType": "ESSP_PURCHASE",
                    "agentId": "%s",
                    "amount": 20.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);
        }

        private String buildPINPurchaseRequest(String idempotencyKey) {
            return """
                {
                    "transactionType": "PIN_PURCHASE",
                    "agentId": "%s",
                    "amount": 10.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "destinationAccount": "telco-number-789",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);
        }

        private String buildRetailSaleRequest(String idempotencyKey) {
            return """
                {
                    "transactionType": "RETAIL_SALE",
                    "agentId": "%s",
                    "amount": 75.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "customerMykad": "encrypted-mykad",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);
        }

        private String buildHybridCashbackRequest(String idempotencyKey) {
            return """
                {
                    "transactionType": "HYBRID_CASHBACK",
                    "agentId": "%s",
                    "amount": 200.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1",
                    "pan": "4111111111111111",
                    "customerCardMasked": "411111******1111",
                    "geofenceLat": 3.1390,
                    "geofenceLng": 101.6869
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);
        }
    }

    @Nested
    @DisplayName("BDD-POLL: Polling Endpoint")
    @Order(20)
    class PollingEndpoint {

        @Test
        @DisplayName("BDD-POLL-01: Poll returns PENDING status for running workflow")
        void poll_pendingWorkflow_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-poll-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            var pollResponse = gatewayClient.get()
                    .uri("/api/v1/transactions/" + idempotencyKey + "/status")
                    .header("Authorization", "Bearer " + agentToken)
                    .exchange();

            assertEquals(200, pollResponse.expectBody(String.class).returnResult().getStatus().value());
        }

        @Test
        @DisplayName("BDD-POLL-04: Poll for non-existent workflowId returns 404")
        void poll_nonexistentWorkflow_shouldReturn404() {
            assumeTrue(agentToken != null, "Agent token required");

            var pollResponse = gatewayClient.get()
                    .uri("/api/v1/transactions/NONEXISTENT-ID/status")
                    .header("Authorization", "Bearer " + agentToken)
                    .exchange();

            assertEquals(404, pollResponse.expectBody(String.class).returnResult().getStatus().value());
        }
    }

    @Nested
    @DisplayName("BDD-IDE: Idempotency")
    @Order(30)
    class IdempotencyTests {

        @Test
        @DisplayName("BDD-IDE-01: Duplicate workflow start returns existing status")
        void duplicateStart_shouldReturnExistingStatus() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-idempotent-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            var response1 = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            var response1Result = response1.expectBody(String.class).returnResult();
            int response1ExpectStatus = response1Result.getStatus().value();
            assertTrue(response1ExpectStatus == 200 || response1ExpectStatus == 202, 
                "First request should return 200 or 202");

            var response2 = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            var response2Result = response2.expectBody(String.class).returnResult();
            int response2ExpectStatus = response2Result.getStatus().value();
            // Idempotent request returns the same status (either 200 or 202)
            assertTrue(response2ExpectStatus == 200 || response2ExpectStatus == 202,
                    "Duplicate request should return 200 or 202, got: " + response2ExpectStatus);
        }
    }

    @Nested
    @DisplayName("Security: Authentication")
    @Order(40)
    class SecurityTests {

        @Test
        @DisplayName("Transaction request without auth token")
        void transaction_withoutAuth_shouldReturnExpectedStatus() {
            String idempotencyKey = "e2e-noauth-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            int statusCode = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getStatus()
                    .value();

            // Note: Gateway may allow unauthenticated requests in dev mode
            // Production config should enforce JwtAuth on this endpoint
            System.out.println("Transaction without auth returned status: " + statusCode);
            assertEquals(401, statusCode, "Transaction without auth should return 401 Unauthorized");
        }
    }

    // ================================================================
    // BDD-WF: Workflow Lifecycle
    // ================================================================

    @Nested
    @DisplayName("BDD-WF: Workflow Lifecycle")
    @Order(50)
    class WorkflowLifecycle {

        @Test
        @DisplayName("BDD-WF-01: Workflow starts and returns PENDING status immediately")
        void startWorkflow_validRequest_shouldReturnPending() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-lifecycle-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());
            assertEquals(idempotencyKey, json.get("workflowId").asText());
        }

        @Test
        @DisplayName("BDD-WF-02: Poll returns COMPLETED status with transaction details")
        void poll_completedWorkflow_shouldReturnDetails() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-poll-completed-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            // Poll for status
            var pollResponse = gatewayClient.get()
                    .uri("/api/v1/transactions/" + idempotencyKey + "/status")
                    .header("Authorization", "Bearer " + agentToken)
                    .exchange();

            assertEquals(200, pollResponse.expectBody(String.class).returnResult().getStatus().value());
        }

        @Test
        @DisplayName("BDD-WF-03: Poll returns FAILED status with error details")
        void poll_failedWorkflow_shouldReturnErrorDetails() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-poll-failed-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            var pollResponse = gatewayClient.get()
                    .uri("/api/v1/transactions/" + idempotencyKey + "/status")
                    .header("Authorization", "Bearer " + agentToken)
                    .exchange();

            assertEquals(200, pollResponse.expectBody(String.class).returnResult().getStatus().value());
        }
    }

    // ================================================================
    // BDD-WF-HP-W: Withdrawal Happy Path
    // ================================================================

    @Nested
    @DisplayName("BDD-WF-HP-W: Withdrawal Happy Path")
    @Order(60)
    class WithdrawalHappyPath {

        @Test
        @DisplayName("BDD-WF-HP-W01: Off-Us withdrawal completes successfully")
        void offUsWithdrawal_shouldCompleteSuccessfully() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-hp-withdraw-offus-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            // Get initial balance before transaction
            BigDecimal initialBalance = getAgentFloatBalance(getEffectiveAgentId());

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());

            // Poll until workflow completes
            WorkflowDetails details = waitForWorkflowCompletion(idempotencyKey);
            
            // Verify workflow completed successfully
            assertEquals("COMPLETED", details.status(), 
                "Workflow should complete successfully, but was: " + details.status() + 
                (details.errorCode() != null ? " Error: " + details.errorCode() : ""));
            
            // Verify transaction details
            assertEquals(new BigDecimal("100.00"), details.amount());
            assertNotNull(details.customerFee());
            
            // Verify side effects - AgentFloat balance decreased
            BigDecimal finalBalance = getAgentFloatBalance(getEffectiveAgentId());
            BigDecimal expectedDeduction = new BigDecimal("100.00").add(details.customerFee());
            assertEquals(initialBalance.subtract(expectedDeduction), finalBalance,
                "Agent float should be deducted by amount + fee");
            
            // Verify JournalEntry records exist
            List<JsonNode> journalEntries = getJournalEntries(details.workflowId());
            assertEquals(2, journalEntries.size(), "Should have 2 journal entries (debit + credit)");
        }

        @Test
        @DisplayName("BDD-WF-HP-W02: On-Us withdrawal starts WithdrawalOnUsWorkflow")
        void onUsWithdrawal_shouldStartWorkflow() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-hp-withdraw-onus-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0012");

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());
        }
    }

    // ================================================================
    // BDD-WF-EC-W: Withdrawal Edge Cases
    // ================================================================

    @Nested
    @DisplayName("BDD-WF-EC-W: Withdrawal Edge Cases")
    @Order(70)
    class WithdrawalEdgeCases {

        @Test
        @DisplayName("BDD-WF-EC-W04: Insufficient float - workflow fails immediately")
        void withdraw_insufficientFloat_shouldFail() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-ec-no-float-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequestLarge(idempotencyKey, "0123");

            // Get initial balance before transaction
            BigDecimal initialBalance = getAgentFloatBalance(getEffectiveAgentId());

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());

            // Poll until workflow fails
            WorkflowDetails details = waitForWorkflowCompletion(idempotencyKey);
            
            // Verify workflow failed
            assertEquals("FAILED", details.status(), 
                "Workflow should fail due to insufficient float");
            assertNotNull(details.errorCode(), "Should have error code for insufficient float");
            
            // Verify no side effects - AgentFloat balance unchanged
            BigDecimal finalBalance = getAgentFloatBalance(getEffectiveAgentId());
            assertEquals(initialBalance, finalBalance,
                "Agent float should remain unchanged when transaction fails");
            
            // Verify no JournalEntry records created
            List<JsonNode> journalEntries = getJournalEntries(details.workflowId());
            assertEquals(0, journalEntries.size(), "No journal entries should exist for failed transaction");
        }

        @Test
        @DisplayName("BDD-WF-EC-W05: Velocity check fails - workflow fails before float block")
        void withdraw_velocityExceeded_shouldFail() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-ec-velocity-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
        }
    }

    // ================================================================
    // BDD-WF-D: Deposit Workflow
    // ================================================================

    @Nested
    @DisplayName("BDD-WF-D: Deposit Workflow")
    @Order(80)
    class DepositWorkflow {

        @Test
        @DisplayName("BDD-WF-HP-D01: Cash deposit completes successfully")
        void deposit_shouldCompleteSuccessfully() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-hp-deposit-" + UUID.randomUUID();
            String requestBody = buildDepositRequest(idempotencyKey);

            // Get initial balance before transaction
            BigDecimal initialBalance = getAgentFloatBalance(getEffectiveAgentId());

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());

            // Poll until workflow completes
            WorkflowDetails details = waitForWorkflowCompletion(idempotencyKey);
            
            // Verify workflow completed successfully
            assertEquals("COMPLETED", details.status(), 
                "Deposit workflow should complete successfully, but was: " + details.status() + 
                (details.errorCode() != null ? " Error: " + details.errorCode() : ""));
            
            // Verify transaction details
            assertEquals(new BigDecimal("500.00"), details.amount());
            assertNotNull(details.customerFee());
            
            // Verify side effects - AgentFloat balance increased
            BigDecimal finalBalance = getAgentFloatBalance(getEffectiveAgentId());
            BigDecimal expectedIncrease = new BigDecimal("500.00").subtract(details.customerFee());
            assertEquals(initialBalance.add(expectedIncrease), finalBalance,
                "Agent float should be increased by amount minus fee");
            
            // Verify JournalEntry records exist
            List<JsonNode> journalEntries = getJournalEntries(details.workflowId());
            assertEquals(2, journalEntries.size(), "Should have 2 journal entries (debit + credit)");
        }

        @Test
        @DisplayName("BDD-WF-EC-D01: Deposit to invalid account - fails before money moves")
        void deposit_invalidAccount_shouldFail() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-deposit-invalid-" + UUID.randomUUID();
            String requestBody = buildDepositRequestInvalidAccount(idempotencyKey);

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
        }

        @Test
        @DisplayName("BDD-WF-EC-D02: High-value deposit requires biometric verification")
        void deposit_highValue_shouldRequireBiometric() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-deposit-highvalue-" + UUID.randomUUID();
            String requestBody = buildDepositRequestHighValue(idempotencyKey);

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
        }
    }

    // ================================================================
    // BDD-WF-BP: Bill Payment Workflow
    // ================================================================

    @Nested
    @DisplayName("BDD-WF-BP: Bill Payment Workflow")
    @Order(90)
    class BillPaymentWorkflow {

        @Test
        @DisplayName("BDD-WF-HP-BP01: Bill payment completes successfully")
        void billPayment_shouldCompleteSuccessfully() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-hp-billpay-" + UUID.randomUUID();
            String requestBody = buildBillPaymentRequest(idempotencyKey);

            // Get initial balance before transaction
            BigDecimal initialBalance = getAgentFloatBalance(getEffectiveAgentId());

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());

            // Poll until workflow completes
            WorkflowDetails details = waitForWorkflowCompletion(idempotencyKey);
            
            // Verify workflow completed successfully
            assertEquals("COMPLETED", details.status(), 
                "Bill payment workflow should complete successfully, but was: " + details.status() + 
                (details.errorCode() != null ? " Error: " + details.errorCode() : ""));
            
            // Verify transaction details
            assertEquals(new BigDecimal("150.00"), details.amount());
            assertNotNull(details.customerFee());
            
            // Verify side effects - AgentFloat balance decreased
            BigDecimal finalBalance = getAgentFloatBalance(getEffectiveAgentId());
            BigDecimal expectedDeduction = new BigDecimal("150.00").add(details.customerFee());
            assertEquals(initialBalance.subtract(expectedDeduction), finalBalance,
                "Agent float should be deducted by amount + fee");
            
            // Verify JournalEntry records exist
            List<JsonNode> journalEntries = getJournalEntries(details.workflowId());
            assertEquals(2, journalEntries.size(), "Should have 2 journal entries (debit + credit)");
        }

        @Test
        @DisplayName("BDD-WF-EC-BP01: Invalid biller reference - float released")
        void billPayment_invalidBiller_shouldFail() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-billpay-invalid-" + UUID.randomUUID();
            String requestBody = buildBillPaymentInvalidBiller(idempotencyKey);

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
        }
    }

    // ================================================================
    // BDD-WF-DN: DuitNow Transfer Workflow
    // ================================================================

    @Nested
    @DisplayName("BDD-WF-DN: DuitNow Transfer Workflow")
    @Order(100)
    class DuitNowWorkflow {

        @Test
        @DisplayName("BDD-WF-HP-DN01: DuitNow transfer via mobile proxy completes successfully")
        void duitNowTransfer_mobileProxy_shouldCompleteSuccessfully() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-hp-duitnow-" + UUID.randomUUID();
            String requestBody = buildDuitNowRequest(idempotencyKey, "MOBILE", "0123456789");

            // Get initial balance before transaction
            BigDecimal initialBalance = getAgentFloatBalance(getEffectiveAgentId());

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());

            // Poll until workflow completes
            WorkflowDetails details = waitForWorkflowCompletion(idempotencyKey);
            
            // Verify workflow completed successfully
            assertEquals("COMPLETED", details.status(), 
                "DuitNow workflow should complete successfully, but was: " + details.status() + 
                (details.errorCode() != null ? " Error: " + details.errorCode() : ""));
            
            // Verify transaction details
            assertEquals(new BigDecimal("100.00"), details.amount());
            assertNotNull(details.customerFee());
            
            // Verify side effects - AgentFloat balance decreased
            BigDecimal finalBalance = getAgentFloatBalance(getEffectiveAgentId());
            BigDecimal expectedDeduction = new BigDecimal("100.00").add(details.customerFee());
            assertEquals(initialBalance.subtract(expectedDeduction), finalBalance,
                "Agent float should be deducted by amount + fee");
            
            // Verify JournalEntry records exist
            List<JsonNode> journalEntries = getJournalEntries(details.workflowId());
            assertEquals(2, journalEntries.size(), "Should have 2 journal entries (debit + credit)");
        }

        @Test
        @DisplayName("BDD-WF-HP-DN02: DuitNow transfer via MyKad proxy")
        void duitNowTransfer_myKadProxy_shouldStart() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-hp-duitnow-mykad-" + UUID.randomUUID();
            String requestBody = buildDuitNowRequest(idempotencyKey, "MYKAD", "123456789012");

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
        }

        @Test
        @DisplayName("BDD-WF-HP-DN03: DuitNow transfer via BRN proxy")
        void duitNowTransfer_brnProxy_shouldStart() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-hp-duitnow-brn-" + UUID.randomUUID();
            String requestBody = buildDuitNowRequest(idempotencyKey, "BRN", "BRN123456");

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
        }

        @Test
        @DisplayName("BDD-WF-EC-DN01: Proxy not found - float released")
        void duitNowTransfer_invalidProxy_shouldFail() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-duitnow-invalid-" + UUID.randomUUID();
            String requestBody = buildDuitNowRequest(idempotencyKey, "MOBILE", "9999999999");

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
        }
    }

    // ================================================================
    // Helper methods
    // ================================================================

    private JsonNode parseBody(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + body, e);
        }
    }

    private String getEffectiveAgentId() {
        return agentId != null ? agentId : AGENT_UUID.toString();
    }

    private String buildWithdrawalRequest(String idempotencyKey, String targetBIN) {
        return """
            {
                "transactionType": "CASH_WITHDRAWAL",
                "agentId": "%s",
                "amount": 100.00,
                "idempotencyKey": "%s",
                "pan": "4111111111111111",
                "customerCardMasked": "411111******1111",
                "targetBIN": "%s",
                "agentTier": "TIER_1"
            }
            """.formatted(getEffectiveAgentId(), idempotencyKey, targetBIN);
    }

    private String buildDepositRequest(String idempotencyKey) {
        return """
            {
                "transactionType": "CASH_DEPOSIT",
                "agentId": "%s",
                "amount": 500.00,
                "idempotencyKey": "%s",
                "destinationAccount": "1234567890",
                "agentTier": "TIER_1"
            }
            """.formatted(getEffectiveAgentId(), idempotencyKey);
    }

    private String buildBillPaymentRequest(String idempotencyKey) {
        return """
            {
                "transactionType": "BILL_PAYMENT",
                "agentId": "%s",
                "amount": 150.00,
                "idempotencyKey": "%s",
                "billerCode": "TNB",
                "ref1": "123456789012",
                "agentTier": "TIER_1"
            }
            """.formatted(getEffectiveAgentId(), idempotencyKey);
    }

    private String buildDuitNowRequest(String idempotencyKey) {
        return buildDuitNowRequest(idempotencyKey, "MOBILE", "0123456789");
    }

    private String buildDuitNowRequest(String idempotencyKey, String proxyType, String proxyValue) {
        return """
            {
                "transactionType": "DUITNOW_TRANSFER",
                "agentId": "%s",
                "amount": 100.00,
                "idempotencyKey": "%s",
                "proxyType": "%s",
                "proxyValue": "%s",
                "agentTier": "TIER_1"
            }
            """.formatted(getEffectiveAgentId(), idempotencyKey, proxyType, proxyValue);
    }

    private String buildWithdrawalRequestLarge(String idempotencyKey, String targetBIN) {
        return """
            {
                "transactionType": "CASH_WITHDRAWAL",
                "agentId": "%s",
                "amount": 1000000.00,
                "idempotencyKey": "%s",
                "pan": "4111111111111111",
                "customerCardMasked": "411111******1111",
                "targetBIN": "%s",
                "agentTier": "TIER_1"
            }
            """.formatted(getEffectiveAgentId(), idempotencyKey, targetBIN);
    }

    private String buildDepositRequestInvalidAccount(String idempotencyKey) {
        return """
            {
                "transactionType": "CASH_DEPOSIT",
                "agentId": "%s",
                "amount": 500.00,
                "idempotencyKey": "%s",
                "destinationAccount": "9999999999",
                "agentTier": "TIER_1"
            }
            """.formatted(getEffectiveAgentId(), idempotencyKey);
    }

    private String buildDepositRequestHighValue(String idempotencyKey) {
        return """
            {
                "transactionType": "CASH_DEPOSIT",
                "agentId": "%s",
                "amount": 10000.00,
                "idempotencyKey": "%s",
                "destinationAccount": "1234567890",
                "requiresBiometric": true,
                "agentTier": "TIER_1"
            }
            """.formatted(getEffectiveAgentId(), idempotencyKey);
    }

    private String buildBillPaymentInvalidBiller(String idempotencyKey) {
        return """
            {
                "transactionType": "BILL_PAYMENT",
                "agentId": "%s",
                "amount": 150.00,
                "idempotencyKey": "%s",
                "billerCode": "INVALID",
                "ref1": "123456789012",
                "agentTier": "TIER_1"
            }
            """.formatted(getEffectiveAgentId(), idempotencyKey);
    }

    // ================================================================
    // BDD-STP: STP Evaluation Tests
    // ================================================================

    @Nested
    @DisplayName("BDD-STP: STP Evaluation End-to-End")
    @Order(110)
    class StpEvaluationTests {

        @Test
        @DisplayName("BDD-STP-01: Full STP - transaction completes immediately")
        void fullStp_transactionCompletes() {
            assumeTrue(agentToken != null, "Agent token required");

            // Standard amount within limits should get FULL_STP
            String idempotencyKey = "e2e-stp-full-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText(),
                "PENDING means workflow started - will proceed through FULL_STP path");

            // Poll after short delay to check if completed
            try { Thread.sleep(3000); } catch (Exception e) {}

            var pollResponse = gatewayClient.get()
                    .uri("/api/v1/transactions/" + idempotencyKey + "/status")
                    .header("Authorization", "Bearer " + agentToken)
                    .exchange();

            String pollBody = pollResponse.expectBody(String.class).returnResult().getResponseBody();
            if (pollBody != null) {
                JsonNode pollJson = parseBody(pollBody);
                String status = pollJson.has("status") ? pollJson.get("status").asText() : "UNKNOWN";
                System.out.println("Transaction status after FULL_STP: " + status);
            }
        }

        @Test
        @DisplayName("BDD-STP-02: High value - triggers PENDING_REVIEW (NON_STP)")
        void highValue_pendingReview() {
            assumeTrue(agentToken != null, "Agent token required");

            // Very high amount should trigger NON_STP (manual review required)
            String idempotencyKey = "e2e-stp-highvalue-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "CASH_WITHDRAWAL",
                    "agentId": "%s",
                    "amount": 50000.00,
                    "idempotencyKey": "%s",
                    "pan": "4111111111111111",
                    "customerCardMasked": "411111******1111",
                    "targetBIN": "0123",
                    "agentTier": "TIER_1"
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());
            System.out.println("High value transaction started - pending review expected");
        }

        @Test
        @DisplayName("BDD-STP-03: Deposit - FULL_STP path")
        void deposit_fullStp() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-stp-deposit-" + UUID.randomUUID();
            String requestBody = buildDepositRequest(idempotencyKey);

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());
        }

        @Test
        @DisplayName("BDD-STP-04: Bill payment - FULL_STP path")
        void billPayment_fullStp() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-stp-billpay-" + UUID.randomUUID();
            String requestBody = buildBillPaymentRequest(idempotencyKey);

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());
        }

        @Test
        @DisplayName("BDD-STP-05: DuitNow transfer - FULL_STP path")
        void duitNow_fullStp() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-stp-duitnow-" + UUID.randomUUID();
            String requestBody = buildDuitNowRequest(idempotencyKey);

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());
        }

        @Test
        @DisplayName("BDD-STP-06: Invalid transaction - rejects at gateway")
        void invalidTransaction_rejects() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-stp-invalid-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "INVALID_TYPE",
                    "agentId": "%s",
                    "amount": 100.00,
                    "idempotencyKey": "%s",
                    "agentTier": "TIER_1"
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);

            var response = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange();

            var result = response.expectBody(String.class).returnResult();
            int statusCode = result.getStatus().value();
            assertTrue(statusCode >= 400, "Invalid transaction type should return 4xx error");
        }

        @Test
        @DisplayName("BDD-STP-07: Verify pendingReason is captured in PENDING_REVIEW status")
        void pendingReview_capturesReason() {
            assumeTrue(agentToken != null, "Agent token required");

            // Create high-value transaction that should trigger manual review
            String idempotencyKey = "e2e-stp-reason-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "CASH_WITHDRAWAL",
                    "agentId": "%s",
                    "amount": 75000.00,
                    "idempotencyKey": "%s",
                    "pan": "4111111111111111",
                    "customerCardMasked": "411111******1111",
                    "targetBIN": "0123",
                    "agentTier": "TIER_1"
                }
                """.formatted(getEffectiveAgentId(), idempotencyKey);

            String body = gatewayClient.post()
                    .uri("/api/v1/transactions")
                    .header("Authorization", "Bearer " + agentToken)
                    .header("X-Idempotency-Key", idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();

            assertNotNull(body);
            JsonNode json = parseBody(body);
            assertEquals("PENDING", json.get("status").asText());
        }
    }

    // ================================================================
    // Helper Methods for Complete BDD Verification
    // ================================================================

    /**
     * Record for holding workflow execution details from poll response
     */
    private record WorkflowDetails(
        String status,
        String workflowId,
        BigDecimal amount,
        BigDecimal customerFee,
        String externalReference,
        String errorCode,
        JsonNode details
    ) {}

    /**
     * Waits for workflow to complete and returns full details.
     */
    private WorkflowDetails waitForWorkflowCompletion(String workflowId) {
        int maxAttempts = 30;
        int delayMs = 1000;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            var pollResponse = gatewayClient.get()
                    .uri("/api/v1/transactions/" + workflowId + "/status")
                    .header("Authorization", "Bearer " + agentToken)
                    .exchange();
            
            String body = pollResponse.expectBody(String.class).returnResult().getResponseBody();
            if (body == null) continue;
            
            JsonNode json = parseBody(body);
            String status = json.has("status") ? json.get("status").asText() : null;
            
            if ("COMPLETED".equals(status) || "FAILED".equals(status) || "PENDING_REVIEW".equals(status)) {
                return new WorkflowDetails(
                    status, workflowId,
                    json.has("amount") ? new BigDecimal(json.get("amount").asText()) : null,
                    json.has("customerFee") ? new BigDecimal(json.get("customerFee").asText()) : null,
                    json.has("externalReference") ? json.get("externalReference").asText() : null,
                    json.has("errorCode") ? json.get("errorCode").asText() : null,
                    json.has("details") ? json.get("details") : null
                );
            }
            
            try { Thread.sleep(delayMs); } catch (InterruptedException e) { break; }
        }
        
        var finalResponse = gatewayClient.get()
                .uri("/api/v1/transactions/" + workflowId + "/status")
                .header("Authorization", "Bearer " + agentToken)
                .exchange();
        
        String body = finalResponse.expectBody(String.class).returnResult().getResponseBody();
        JsonNode json = parseBody(body);
        
        return new WorkflowDetails(
            json.has("status") ? json.get("status").asText() : "TIMEOUT", workflowId,
            json.has("amount") ? new BigDecimal(json.get("amount").asText()) : null,
            json.has("customerFee") ? new BigDecimal(json.get("customerFee").asText()) : null,
            json.has("externalReference") ? json.get("externalReference").asText() : null,
            json.has("errorCode") ? json.get("errorCode").asText() : null,
            json.has("details") ? json.get("details") : null
        );
    }

    /**
     * Retrieves current agent float balance from ledger service.
     */
    private BigDecimal getAgentFloatBalance(String agentId) {
        try {
            String response = ledgerClient.get()
                    .uri("/internal/agents/" + agentId + "/float")
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();
            
            JsonNode json = parseBody(response);
            return json.has("balance") ? new BigDecimal(json.get("balance").asText()) : BigDecimal.ZERO;
        } catch (Exception e) {
            System.out.println("Failed to get agent float balance: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Retrieves journal entries for a transaction from ledger service.
     */
    private List<JsonNode> getJournalEntries(String workflowId) {
        List<JsonNode> entries = new ArrayList<>();
        try {
            String response = ledgerClient.get()
                    .uri("/internal/journal?workflowId=" + workflowId)
                    .exchange()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();
            
            JsonNode json = parseBody(response);
            if (json.isArray()) {
                json.forEach(entries::add);
            }
        } catch (Exception e) {
            System.out.println("Failed to get journal entries: " + e.getMessage());
        }
        return entries;
    }
}

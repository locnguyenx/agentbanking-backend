package com.agentbanking.gateway.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for External API endpoints (POS Terminal → Gateway)
 * Tests verify all design-specified endpoints are accessible and return expected responses
 * 
 * Run against live Docker services:
 * docker-compose --profile infra --profile backend --profile gateway up -d
 * ./gradlew :gateway:test --tests "ExternalApiIntegrationTest"
 */
class ExternalApiIntegrationTest {

    private final WebTestClient webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:8080")
            .build();

    private static final String JWT_TOKEN = "Bearer test-jwt-token";
    private static final String IDEMPOTENCY_KEY = "550e8400-e29b-41d4-a716-446655440000";
    private static final String POS_TERMINAL_ID = "POS-001";
    private static final String GPS_LAT = "3.1390";
    private static final String GPS_LNG = "101.6869";

    // ========== WITHDRAWAL ==========

    @Test
    void withdrawal_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "amount": 500.00,
                "card_data": "encrypted_card_blob",
                "pin_block": "dukpt_encrypted_pin_block",
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/withdrawal")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void withdrawal_withoutAuth_shouldReturn401() {
        String requestBody = """
            {
                "amount": 500.00,
                "card_data": "encrypted_card_blob",
                "pin_block": "dukpt_encrypted_pin_block",
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/withdrawal")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ========== DEPOSIT ==========

    @Test
    void deposit_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "amount": 1000.00,
                "destination_account": "1234567890",
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/deposit")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ========== BALANCE INQUIRY ==========

    @Test
    void balanceInquiry_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "card_data": "encrypted_card_blob",
                "pin_block": "dukpt_encrypted_pin_block"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/balance-inquiry")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void agentBalance_shouldReturn401WithoutValidToken() {
        webTestClient.get()
                .uri("/api/v1/agent/balance")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ========== KYC ==========

    @Test
    void kycVerify_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "mykad_number": "123456789012"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/kyc/verify")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void kycBiometric_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "verification_id": "550e8400-e29b-41d4-a716-446655440000",
                "biometric_data": "encrypted_thumbprint_blob"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/kyc/biometric")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ========== BILL PAYMENT ==========

    @Test
    void billPay_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "biller_code": "JOMPAY",
                "ref1": "1234567890",
                "ref2": "REF2",
                "amount": 150.00,
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/bill/pay")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ========== TOPUP ==========

    @Test
    void topup_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "telco": "CELCOM",
                "phone_number": "0123456789",
                "amount": 10.00,
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/topup")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ========== DUITNOW ==========

    @Test
    void duitNowTransfer_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "proxy_type": "PHONE",
                "proxy_value": "0123456789",
                "amount": 500.00,
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/transfer/duitnow")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ========== RETAIL ==========

    @Test
    void retailSale_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "amount": 100.00,
                "card_data": "encrypted_card_blob",
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/retail/sale")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void pinPurchase_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "product_code": "VOUCHER-001",
                "amount": 50.00,
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/retail/pin-purchase")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void retailCashback_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "purchase_amount": 50.00,
                "cashback_amount": 100.00,
                "card_data": "encrypted_card_blob",
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/retail/cashback")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ========== EWALLET ==========

    @Test
    void eWalletWithdraw_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "wallet_provider": "GRABPAY",
                "wallet_id": "wallet_123",
                "amount": 200.00,
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/ewallet/withdraw")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void eWalletTopup_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "wallet_provider": "GRABPAY",
                "wallet_id": "wallet_123",
                "amount": 100.00,
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/ewallet/topup")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ========== ESSP ==========

    @Test
    void esspPurchase_shouldReturn401WithoutValidToken() {
        String requestBody = """
            {
                "amount": 50.00,
                "currency": "MYR"
            }
            """;

        webTestClient.post()
                .uri("/api/v1/essp/purchase")
                .header("Authorization", JWT_TOKEN)
                .header("X-Idempotency-Key", IDEMPOTENCY_KEY)
                .header("X-POS-Terminal-Id", POS_TERMINAL_ID)
                .header("X-GPS-Latitude", GPS_LAT)
                .header("X-GPS-Longitude", GPS_LNG)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ========== BACKOFFICE ==========

    @Test
    void backofficeDashboard_shouldReturn401WithoutValidToken() {
        webTestClient.get()
                .uri("/api/v1/backoffice/dashboard")
                .header("Authorization", JWT_TOKEN)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void backofficeAgentsList_shouldReturn401WithoutValidToken() {
        webTestClient.get()
                .uri("/api/v1/backoffice/agents?page=0&size=20")
                .header("Authorization", JWT_TOKEN)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void backofficeTransactions_shouldReturn401WithoutValidToken() {
        webTestClient.get()
                .uri("/api/v1/backoffice/transactions?page=0&size=50")
                .header("Authorization", JWT_TOKEN)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void backofficeSettlement_shouldReturn401WithoutValidToken() {
        webTestClient.get()
                .uri("/api/v1/backoffice/settlement?date=2026-03-29")
                .header("Authorization", JWT_TOKEN)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void backofficeKycReviewQueue_shouldReturn401WithoutValidToken() {
        webTestClient.get()
                .uri("/api/v1/backoffice/kyc/review-queue?page=0&size=20")
                .header("Authorization", JWT_TOKEN)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void backofficeAuditLogs_shouldReturn401WithoutValidToken() {
        webTestClient.get()
                .uri("/api/v1/backoffice/audit-logs?page=0&size=50")
                .header("Authorization", JWT_TOKEN)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ========== API DOCS ==========

    @Test
    void openApiDocs_shouldReturn200() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.openapi").isEqualTo("3.0.3")
                .jsonPath("$.info.title").isEqualTo("Agent Banking Platform API")
                .jsonPath("$.paths").isNotEmpty();
    }

    @Test
    void openApiDocsWithTrailingSlash_shouldReturn200() {
        webTestClient.get()
                .uri("/v3/api-docs/")
                .exchange()
                .expectStatus().isOk();
    }
}

package com.agentbanking.gateway.integration.gateway;

import com.agentbanking.gateway.integration.BaseGatewayIntegrationTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Refactored Integration test for Gateway routing verification.
 * Uses @SpringBootTest and WireMock for isolated testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
@ActiveProfiles("tc")
public class ExternalApiIntegrationTest extends BaseGatewayIntegrationTest {

    @LocalServerPort
    private int port;

    private static WireMockServer wireMockServer;
    private WebTestClient webClient;

    private static final String JWT_SECRET = "your-super-secret-jwt-key-change-in-production-minimum-32-chars-long";

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setupClient() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        wireMockServer.resetAll();
    }

    @DynamicPropertySource
    static void configureGatewayRoutes(DynamicPropertyRegistry registry) {
        // Point all service URIs to our WireMock server
        String wireMockUrl = "http://localhost:" + wireMockServer.port();
        
        // Explicitly set the service URLs used by the existing routes
        registry.add("auth-service.url", () -> wireMockUrl);
        registry.add("biller-service.url", () -> wireMockUrl);
        registry.add("switch-service.url", () -> wireMockUrl);
        registry.add("rules-service.url", () -> wireMockUrl);
        registry.add("onboarding-service.url", () -> wireMockUrl);
        registry.add("ledger-service.url", () -> wireMockUrl);
        registry.add("orchestrator-service.url", () -> wireMockUrl);
        
        // Point self-referencing routes back to the running test server
        registry.add("gateway-service.url", () -> "http://localhost:" + wireMockServer.port());
        
        // Also set JWT secret for auth filter
        registry.add("JWT_SECRET", () -> JWT_SECRET);
    }

    private String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    @DisplayName("Biller: Proxy Enquiry - /api/v1/transfer/proxy/enquiry")
    void verifyBillerProxyEnquiry() {
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/transfer/proxy/enquiry"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"SUCCESS\"}")));

        String token = generateToken("agent001", "ROLE_AGENT");
        
        webClient.get()
                .uri("/api/v1/transfer/proxy/enquiry?proxyType=MOBILE&proxyValue=0123456789")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Switch: Balance Inquiry - /api/v1/balance-inquiry")
    void verifySwitchBalanceInquiry() {
        wireMockServer.stubFor(post(urlPathEqualTo("/internal/balance-inquiry"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"SUCCESS\", \"balance\": 1000.0}")));

        String token = generateToken("agent001", "ROLE_AGENT");
        
        webClient.post()
                .uri("/api/v1/balance-inquiry")
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"accountNumber\":\"12345\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Rules: Fees - /api/v1/rules/fees")
    void verifyRulesFees() {
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/fees"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"baseFee\": 2.5}")));

        String token = generateToken("agent001", "ROLE_AGENT");
        
        webClient.get()
                .uri("/api/v1/rules/fees?transactionType=CASH_WITHDRAWAL&agentTier=MICRO")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.baseFee").isEqualTo(2.5);
    }

    @Test
    @DisplayName("Onboarding: Submit Application - /api/v1/onboarding/submit-application")
    void verifyOnboardingSubmitApplication() {
        wireMockServer.stubFor(post(urlPathEqualTo("/internal/onboarding/application"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"applicationId\":\"APP-123\"}")));

        String token = generateToken("agent001", "ROLE_AGENT");
        
        webClient.post()
                .uri("/api/v1/onboarding/submit-application")
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"name\":\"Test\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.applicationId").isEqualTo("APP-123");
    }

    @Test
    @DisplayName("Auth: Should return 401 for missing token")
    void verifyUnauthorized() {
        webClient.get()
                .uri("/api/v1/agent/balance")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Onboarding: Verify MyKad - /api/v1/onboarding/verify-mykad")
    void verifyOnboardingVerifyMyKad() {
        wireMockServer.stubFor(post(urlPathEqualTo("/internal/verify-mykad"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"verified\":true,\"icNumber\":\"***\"}")));

        String token = generateToken("agent001", "ROLE_AGENT");
        
        webClient.post()
                .uri("/api/v1/onboarding/verify-mykad")
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"icNumber\":\"123456789012\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.verified").isEqualTo(true);
    }

    @Test
    @DisplayName("Onboarding: KYC Biometric - /api/v1/kyc/biometric")
    void verifyKycBiometric() {
        wireMockServer.stubFor(post(urlPathEqualTo("/internal/biometric"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"biometricVerified\":true}")));

        String token = generateToken("agent001", "ROLE_AGENT");
        
        webClient.post()
                .uri("/api/v1/kyc/biometric")
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"fingerprint\":\"base64data\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.biometricVerified").isEqualTo(true);
    }

    @Test
    @DisplayName("Onboarding: KYC Verify - /api/v1/kyc/verify")
    void verifyKycVerify() {
        wireMockServer.stubFor(post(urlPathEqualTo("/internal/verify-mykad"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"verified\":true}")));

        String token = generateToken("agent001", "ROLE_AGENT");
        
        webClient.post()
                .uri("/api/v1/kyc/verify")
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"mykadNumber\":\"123456789012\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.verified").isEqualTo(true);
    }

    @Test
    @DisplayName("Onboarding: KYC Verify - /api/v1/kyc/verify - Unauthorized")
    void verifyKycVerifyUnauthorized() {
        webClient.post()
                .uri("/api/v1/kyc/verify")
                .bodyValue("{\"mykadNumber\":\"123456789012\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Onboarding: Compliance Status - /api/v1/compliance/status")
    void verifyComplianceStatus() {
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/compliance/status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"APPROVED\"}")));

        String token = generateToken("agent001", "ROLE_AGENT");
        
        webClient.get()
                .uri("/api/v1/compliance/status?agentId=agent001")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Rules: Transaction Quote - /api/v1/transactions/quote")
    void verifyTransactionQuote() {
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/transactions/quote"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"totalAmount\":1050.00,\"fee\":50.00}")));

        String token = generateToken("agent001", "ROLE_AGENT");
        
        webClient.get()
                .uri("/api/v1/transactions/quote?transactionType=CASH_WITHDRAWAL&amount=1000")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalAmount").isEqualTo(1050.00);
    }

    @Test
    @DisplayName("Ledger: Ledger Transactions - /api/v1/backoffice/ledger-transactions")
    void verifyLedgerTransactions() {
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/backoffice/ledger-transactions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transactions\":[]}")));

        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.get()
                .uri("/api/v1/backoffice/ledger-transactions?page=0&size=20")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.transactions").isArray();
    }

    @Test
    @DisplayName("Ledger: Dashboard - /api/v1/backoffice/dashboard")
    void verifyDashboard() {
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/backoffice/dashboard"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"totalAgents\":100,\"activeAgents\":75}")));

        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.get()
                .uri("/api/v1/backoffice/dashboard")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalAgents").isEqualTo(100);
    }

    @Test
    @DisplayName("Onboarding: Agents List - /api/v1/backoffice/agents")
    void verifyAgentsList() {
        wireMockServer.stubFor(get(urlPathEqualTo("/backoffice/agents"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"agents\":[]}")));

        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.get()
                .uri("/api/v1/backoffice/agents?page=0&size=20")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.agents").isArray();
    }

    @Test
    @DisplayName("Ledger: Settlement - /api/v1/backoffice/settlement")
    void verifySettlement() {
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/backoffice/settlement"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"settlements\":[]}")));

        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.get()
                .uri("/api/v1/backoffice/settlement?date=2026-04-10")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.settlements").isArray();
    }

    @Test
    @DisplayName("Onboarding: KYC Review Queue - /api/v1/backoffice/kyc/review-queue")
    void verifyKycReviewQueue() {
        wireMockServer.stubFor(get(urlPathEqualTo("/internal/kyc/review-queue"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"queue\":[]}")));

        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.get()
                .uri("/api/v1/backoffice/kyc/review-queue?page=0&size=20")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.queue").isArray();
    }

    @Test
    @DisplayName("Ledger: Discrepancy Maker Action - /api/v1/backoffice/discrepancy/*/maker-action")
    void verifyDiscrepancyMakerAction() {
        wireMockServer.stubFor(post(urlPathEqualTo("/internal/reconciliation/discrepancy/maker-propose"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"caseId\":\"DISC-001\",\"status\":\"PENDING_CHECKER\"}")));

        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.post()
                .uri("/api/v1/backoffice/discrepancy/DISC-001/maker-action")
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"resolution\":\"ADJUST\",\"amount\":100.00}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.caseId").isEqualTo("DISC-001");
    }

    @Test
    @DisplayName("Ledger: Discrepancy Checker Approve - /api/v1/backoffice/discrepancy/*/checker-approve")
    void verifyDiscrepancyCheckerApprove() {
        wireMockServer.stubFor(post(urlPathEqualTo("/internal/reconciliation/discrepancy/checker-approve"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"caseId\":\"DISC-001\",\"status\":\"APPROVED\"}")));

        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.post()
                .uri("/api/v1/backoffice/discrepancy/DISC-001/checker-approve")
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"approved\":true}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.caseId").isEqualTo("DISC-001")
                .jsonPath("$.status").isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("Ledger: Discrepancy Checker Reject - /api/v1/backoffice/discrepancy/*/checker-reject")
    void verifyDiscrepancyCheckerReject() {
        wireMockServer.stubFor(post(urlPathEqualTo("/internal/reconciliation/discrepancy/checker-reject"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"caseId\":\"DISC-001\",\"status\":\"REJECTED\"}")));

        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.post()
                .uri("/api/v1/backoffice/discrepancy/DISC-001/checker-reject")
                .header("Authorization", "Bearer " + token)
                .bodyValue("{\"reason\":\"Invalid documentation\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.caseId").isEqualTo("DISC-001")
                .jsonPath("$.status").isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("Auth: Audit Logs - /api/v1/backoffice/audit-logs")
    void verifyAuditLogs() {
        wireMockServer.stubFor(get(urlPathEqualTo("/auth/audit/logs"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"logs\":[]}")));

        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.get()
                .uri("/api/v1/backoffice/audit-logs?page=0&size=20")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.logs").isArray();
    }

    @Test
    @DisplayName("Auth: Admin Users - /api/v1/backoffice/admin/users")
    void verifyAdminUsers() {
        wireMockServer.stubFor(get(urlPathEqualTo("/auth/users"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"users\":[]}")));

        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.get()
                .uri("/api/v1/backoffice/admin/users?page=0&size=20")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.users").isArray();
    }

    @Test
    @DisplayName("Auth: Admin Roles - /api/v1/backoffice/admin/roles")
    void verifyAdminRoles() {
        wireMockServer.stubFor(get(urlPathEqualTo("/auth/roles"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"roles\":[{\"name\":\"ROLE_ADMIN\"}]}")));

        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.get()
                .uri("/api/v1/backoffice/admin/roles")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.roles").isArray()
                .jsonPath("$.roles[0].name").isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Gateway: Health All - /api/v1/admin/health/all - requires E2E")
    void verifyHealthAll() {
        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.get()
                .uri("/api/v1/admin/health/all")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Gateway: Health Service - /api/v1/admin/health/{service} - requires E2E")
    void verifyHealthService() {
        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.get()
                .uri("/api/v1/admin/health/auth-service")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Gateway: Metrics - /api/v1/admin/metrics/{service} - requires E2E")
    void verifyMetrics() {
        String token = generateToken("admin001", "ROLE_ADMIN");
        
        webClient.get()
                .uri("/api/v1/admin/metrics/auth-service")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }
}

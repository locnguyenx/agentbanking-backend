package com.agentbanking.gateway.integration.backoffice;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.*;

/**
 * Phase 7f: KYC Review Queue Tests
 * 
 * Tests KYC review queue through the onboarding service.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(45)
class KycReviewQueueTest extends BaseIntegrationTest {

    @Test
    @Order(1)
    @DisplayName("BDD-KRQ01 [HP]: Get KYC review queue")
    void getKycReviewQueue() {
        var response = gatewayGet("/api/v1/backoffice/kyc/review-queue?page=0&size=20",
                TestContext.adminToken);

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("KYC review queue status: " + (status != null ? status.value() : "null"));
        System.out.println("KYC review queue response: " + responseBody);
    }

    @Test
    @Order(2)
    @DisplayName("BDD-KRQ01-EC-02: Review queue without auth returns 401")
    void reviewQueue_withoutAuth_returns401() {
        var response = gatewayGetNoAuth("/api/v1/backoffice/kyc/review-queue");

        var status = response.expectBody(String.class).returnResult().getStatus();
        Assertions.assertEquals(401, status != null ? status.value() : 0);
    }
}

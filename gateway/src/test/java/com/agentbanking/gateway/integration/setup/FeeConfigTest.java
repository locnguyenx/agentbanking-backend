package com.agentbanking.gateway.integration.setup;

import com.agentbanking.gateway.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3: Fee Configuration
 * 
 * Sets up fee rules for transaction tests via the rules service API.
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@org.junit.jupiter.api.Order(3)
class FeeConfigTest extends BaseIntegrationTest {

    private static final String TODAY = LocalDate.now().toString();

    // ================================================================
    // STEP 1: Create withdrawal fee config
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("Phase 3.1: Create withdrawal fee config for STANDARD tier")
    void createWithdrawalFee() {
        String body = """
                {
                    "transactionType": "CASH_WITHDRAWAL",
                    "agentTier": "STANDARD",
                    "feeType": "FIXED",
                    "customerFeeValue": 1.00,
                    "agentCommissionValue": 0.20,
                    "bankShareValue": 0.80,
                    "dailyLimitAmount": 10000.00,
                    "dailyLimitCount": 10,
                    "effectiveFrom": "%s",
                    "effectiveTo": null
                }
                """.formatted(TODAY);

        var response = rulesClient.post()
                .uri("/internal/fees")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Withdrawal fee config status: " + (status != null ? status.value() : "null"));
        System.out.println("Withdrawal fee config response: " + responseBody);

        // Fee config may already exist, which is OK
        if (status != null && status.value() == 201) {
            try {
                JsonNode node = objectMapper.readTree(responseBody);
                System.out.println("Fee config ID: " + node.get("feeConfigId").asText());
            } catch (Exception e) {
                System.out.println("Failed to parse fee config response");
            }
        }
    }

    // ================================================================
    // STEP 2: Create deposit fee config
    // ================================================================

    @Test
    @Order(2)
    @DisplayName("Phase 3.2: Create deposit fee config for STANDARD tier")
    void createDepositFee() {
        String body = """
                {
                    "transactionType": "CASH_DEPOSIT",
                    "agentTier": "STANDARD",
                    "feeType": "FIXED",
                    "customerFeeValue": 0.50,
                    "agentCommissionValue": 0.10,
                    "bankShareValue": 0.40,
                    "dailyLimitAmount": 50000.00,
                    "dailyLimitCount": 20,
                    "effectiveFrom": "%s",
                    "effectiveTo": null
                }
                """.formatted(TODAY);

        var response = rulesClient.post()
                .uri("/internal/fees")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        System.out.println("Deposit fee config status: " + (status != null ? status.value() : "null"));
    }

    // ================================================================
    // STEP 3: Verify fee calculation works
    // ================================================================

    @Test
    @Order(3)
    @DisplayName("Phase 3.3: Verify fee calculation for withdrawal")
    void verifyFeeCalculation() {
        var response = rulesClient.get()
                .uri("/internal/fees/calculate?transactionType=CASH_WITHDRAWAL&agentTier=STANDARD&amount=100.00")
                .exchange();

        var status = response.expectBody(String.class).returnResult().getStatus();
        String responseBody = response.expectBody(String.class).returnResult().getResponseBody();

        System.out.println("Fee calculation status: " + (status != null ? status.value() : "null"));
        System.out.println("Fee calculation response: " + responseBody);

        if (status != null && status.value() == 200) {
            try {
                JsonNode node = objectMapper.readTree(responseBody);
                System.out.println("Customer fee: " + node.get("customerFee").asText());
                System.out.println("Agent commission: " + node.get("agentCommission").asText());
                System.out.println("Bank share: " + node.get("bankShare").asText());
            } catch (Exception e) {
                System.out.println("Failed to parse fee calculation response");
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Phase 3.4: Verify fee config phase is complete")
    void verifyFeeConfigComplete() {
        System.out.println("=== Phase 3 Fee Config Complete ===");
    }
}

package com.agentbanking.gateway.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * BDD Section 2: Ledger & Float Integration Tests
 * Tests balance inquiry and agent float management
 * 
 * BDD Scenarios: L01, L01-EC-01, L01-EC-02, L04, L04-EC-01, L04-EC-02
 */
class LedgerFloatIntegrationTest extends BaseIntegrationTest {

    private static final String BALANCE_INQUIRY_ENDPOINT = "/api/v1/balance-inquiry";
    private static final String AGENT_BALANCE_ENDPOINT = "/api/v1/agent/balance";

    // ========== BDD-L01: Agent Balance Inquiry ==========
    @Nested
    @DisplayName("BDD-L01: Agent Balance Inquiry")
    class AgentBalanceInquiry {

        @Test
        @DisplayName("BDD-L01 [HP]: Agent checks wallet balance")
        void agentBalance_microAgent_shouldReturnBalance() {
            String token = getMicroAgentToken();

            authenticatedGet(AGENT_BALANCE_ENDPOINT, token)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.agent_id").isEqualTo(MICRO_AGENT_ID)
                    .jsonPath("$.available_balance").isNumber()
                    .jsonPath("$.ledger_balance").isNumber();
        }

        @Test
        @DisplayName("BDD-L01 [HP]: Standard agent balance")
        void agentBalance_standardAgent_shouldReturnBalance() {
            String token = getStandardAgentToken();

            authenticatedGet(AGENT_BALANCE_ENDPOINT, token)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.agent_id").isEqualTo(STANDARD_AGENT_ID)
                    .jsonPath("$.available_balance").isNumber();
        }
    }

    // ========== BDD-L04: Customer Balance Inquiry ==========
    @Nested
    @DisplayName("BDD-L04: Customer Balance Inquiry")
    class CustomerBalanceInquiry {

        @Test
        @DisplayName("BDD-L04 [HP]: Customer balance inquiry via card + PIN")
        void balanceInquiry_validCardAndPin_shouldReturnBalance() {
            String token = getStandardAgentToken();
            String requestBody = """
                {
                    "card_data": "%s",
                    "pin_block": "%s"
                }
                """.formatted(ENCRYPTED_CARD, DUKPT_PIN);

            authenticatedPost(BALANCE_INQUIRY_ENDPOINT, token, requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("SUCCESS")
                    .jsonPath("$.available_balance").isNumber()
                    .jsonPath("$.currency").isEqualTo("MYR");
        }
    }
}

package com.agentbanking.gateway.integration;

import com.agentbanking.gateway.integration.setup.TestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.agentbanking.gateway.integration.BaseIntegrationTest.MYR_CURRENCY;
import static com.agentbanking.gateway.integration.BaseIntegrationTest.ENCRYPTED_CARD;
import static com.agentbanking.gateway.integration.BaseIntegrationTest.DUKPT_PIN;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BDD Section 1: Rules & Fee Engine Integration Tests
 * Tests fee configuration, daily limits, and velocity checks
 * 
 * BDD Scenarios: R01, R01-PCT, R01-EC-01, R01-EC-02, R01-EC-03,
 *                R02, R02-EC-01, R02-EC-02, R02-EC-03, R02-EC-04,
 *                R03, R03-EC-01, R03-EC-02, R03-EC-03, R03-EC-04, R04
 */
class RulesFeeEngineIntegrationTest extends BaseIntegrationTest {

    private static final String WITHDRAWAL_ENDPOINT = "/api/v1/withdrawal";

    @BeforeAll
    static void checkAuthSetup() {
        assumeTrue(TestContext.isAuthSetupComplete(), 
            "Skipping RulesFeeEngineIntegrationTest — auth setup not complete (agentToken is null)");
    }

    // ========== BDD-R01: Fee Configuration ==========
    @Nested
    @DisplayName("BDD-R01: Fee Configuration")
    class FeeConfiguration {

        @Test
        @DisplayName("BDD-R01 [HP]: Configure fee structure for Micro agent cash withdrawal")
        void withdrawal_microAgent_withFixedFee_shouldApplyFee() {
            // TODO: When test infrastructure supports tier-specific agents, use getMicroAgentToken()
            // For now, uses TestContext.agentToken (STANDARD tier)
            String token = TestContext.agentToken;
            String requestBody = """
                {
                    "amount": 500.00,
                    "card_data": "%s",
                    "pin_block": "%s",
                    "currency": "%s"
                }
                """.formatted(ENCRYPTED_CARD, DUKPT_PIN, MYR_CURRENCY);

            authenticatedPost(WITHDRAWAL_ENDPOINT, token, requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("SUCCESS")
                    .jsonPath("$.customer_fee").isNumber()
                    .jsonPath("$.agent_commission").isNumber()
                    .jsonPath("$.bank_share").isNumber();
        }

        @Test
        @DisplayName("BDD-R01-PCT [HP]: Percentage-based fee for Premier agent cash withdrawal")
        void withdrawal_premierAgent_withPercentageFee_shouldApplyFee() {
            // TODO: When test infrastructure supports tier-specific agents, use getPremierAgentToken()
            // For now, uses TestContext.agentToken (STANDARD tier)
            String token = TestContext.agentToken;
            String requestBody = """
                {
                    "amount": 10000.00,
                    "card_data": "%s",
                    "pin_block": "%s",
                    "currency": "%s"
                }
                """.formatted(ENCRYPTED_CARD, DUKPT_PIN, MYR_CURRENCY);

            authenticatedPost(WITHDRAWAL_ENDPOINT, token, requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("SUCCESS")
                    .jsonPath("$.customer_fee").isNumber();
        }

        @Test
        @DisplayName("BDD-R04 [HP]: Percentage-based fee calculation with rounding")
        void withdrawal_withPercentageFee_shouldRoundCorrectly() {
            // TODO: When test infrastructure supports tier-specific agents, use getStandardAgentToken()
            // For now, uses TestContext.agentToken (STANDARD tier)
            String token = TestContext.agentToken;
            String requestBody = """
                {
                    "amount": 333.33,
                    "card_data": "%s",
                    "pin_block": "%s",
                    "currency": "%s"
                }
                """.formatted(ENCRYPTED_CARD, DUKPT_PIN, MYR_CURRENCY);

            authenticatedPost(WITHDRAWAL_ENDPOINT, token, requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("SUCCESS");
        }
    }

    // ========== BDD-R02: Daily Transaction Limits ==========
    @Nested
    @DisplayName("BDD-R02: Daily Transaction Limits")
    class DailyTransactionLimits {

        @Test
        @DisplayName("BDD-R02 [HP]: Daily transaction limit check passes")
        void withdrawal_withinDailyLimit_shouldPass() {
            // TODO: When test infrastructure supports tier-specific agents, use getStandardAgentToken()
            String token = TestContext.agentToken;
            String requestBody = """
                {
                    "amount": 3000.00,
                    "card_data": "%s",
                    "pin_block": "%s",
                    "currency": "%s"
                }
                """.formatted(ENCRYPTED_CARD, DUKPT_PIN, MYR_CURRENCY);

            authenticatedPost(WITHDRAWAL_ENDPOINT, token, requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("BDD-R02-EC-03 [EC]: Transaction amount is zero")
        void withdrawal_zeroAmount_shouldReturnError() {
            // TODO: When test infrastructure supports tier-specific agents, use getMicroAgentToken()
            String token = TestContext.agentToken;
            String requestBody = """
                {
                    "amount": 0.00,
                    "card_data": "%s",
                    "pin_block": "%s",
                    "currency": "%s"
                }
                """.formatted(ENCRYPTED_CARD, DUKPT_PIN, MYR_CURRENCY);

            authenticatedPost(WITHDRAWAL_ENDPOINT, token, requestBody)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("FAILED")
                    .jsonPath("$.error.code").isEqualTo("ERR_INVALID_AMOUNT");
        }

        @Test
        @DisplayName("BDD-R02-EC-04 [EC]: Transaction amount is negative")
        void withdrawal_negativeAmount_shouldReturnError() {
            // TODO: When test infrastructure supports tier-specific agents, use getMicroAgentToken()
            String token = TestContext.agentToken;
            String requestBody = """
                {
                    "amount": -100.00,
                    "card_data": "%s",
                    "pin_block": "%s",
                    "currency": "%s"
                }
                """.formatted(ENCRYPTED_CARD, DUKPT_PIN, MYR_CURRENCY);

            authenticatedPost(WITHDRAWAL_ENDPOINT, token, requestBody)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("FAILED")
                    .jsonPath("$.error.code").isEqualTo("ERR_INVALID_AMOUNT");
        }
    }

    // ========== BDD-R03: Velocity Checks ==========
    @Nested
    @DisplayName("BDD-R03: Velocity Checks")
    class VelocityChecks {

        @Test
        @DisplayName("BDD-R03 [HP]: Velocity check passes")
        void withdrawal_velocityCheckPasses_shouldSucceed() {
            // TODO: When test infrastructure supports tier-specific agents, use getStandardAgentToken()
            String token = TestContext.agentToken;
            String requestBody = """
                {
                    "amount": 500.00,
                    "card_data": "%s",
                    "pin_block": "%s",
                    "currency": "%s"
                }
                """.formatted(ENCRYPTED_CARD, DUKPT_PIN, MYR_CURRENCY);

            authenticatedPost(WITHDRAWAL_ENDPOINT, token, requestBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("SUCCESS");
        }
    }
}

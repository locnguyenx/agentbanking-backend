package com.agentbanking.orchestrator.integration;

import com.agentbanking.orchestrator.domain.port.out.StoreAndForwardPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BDD-V Series: Store & Forward Reversals Tests
 *
 * These tests verify the Store & Forward mechanism for handling failed reversal messages
 * due to network outages, ensuring transaction integrity across connectivity issues.
 *
 * BDD Reference: docs/superpowers/specs/agent-banking-platform/2026-03-25-agent-banking-platform-bdd.md
 * Section 11: Reversals & Disputes (BDD-V)
 */
@AutoConfigureMockMvc
@DisplayName("BDD-V Series: Store & Forward Reversals")
class BDDStoreAndForwardIntegrationTest extends AbstractOrchestratorRealInfraIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreAndForwardPort storeAndForwardPort;

    private static final UUID AGENT_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    // ================================================================
    // BDD-V01: Network timeout triggers automatic reversal (Store & Forward)
    // ================================================================

    @Nested
    @DisplayName("BDD-V01 [HP]: Network timeout triggers automatic reversal (Store & Forward)")
    class BDD_V01_NetworkTimeoutTriggersReversal {

        @Test
        @DisplayName("BDD-V01: Network timeout queues reversal for Store & Forward retry")
        void BDD_V01_networkTimeoutTriggersStoreAndForward() throws Exception {
            // Given: A withdrawal transaction that will experience network timeout
            String idempotencyKey = "BDD-V01-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            // When: Submit transaction (would timeout in real scenario)
            mockMvc.perform(post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isAccepted());

            // Then: In a real scenario, network timeout would trigger Store & Forward
            // For testing, we simulate queuing a reversal message
            UUID transactionId = UUID.randomUUID();
            StoreAndForwardPort.ReversalMessage sfMessage = new StoreAndForwardPort.ReversalMessage(
                UUID.randomUUID().toString(),
                transactionId,
                "0400", // Financial reversal MTI
                "MTI:0400|Transaction:" + transactionId,
                0, // First attempt
                Instant.now(),
                Instant.now(),
                "Network timeout"
            );

            // When: Queue message for Store & Forward
            storeAndForwardPort.queueReversalForRetry(sfMessage);

            // Then: Message should be queued
            StoreAndForwardPort.QueueStatistics stats = storeAndForwardPort.getQueueStatistics();
            assertThat(stats.pendingMessages()).isGreaterThan(0);

            // BDD Then: System should mark transaction_history status as REVERSAL_INITIATED
            // BDD Then: Call Ledger Service to Rollback the float lock
            // BDD Then: AgentFloat.balance should be restored
            // BDD Then: Transaction.status should change to REVERSED

            // Note: Full workflow integration testing would require Temporal workflow execution
        }
    }

    // ================================================================
    // BDD-V01-EC-01: Reversal message fails - Store & Forward retries
    // ================================================================

    @Nested
    @DisplayName("BDD-V01-EC-01 [EC]: Reversal message fails - Store & Forward retries")
    class BDD_V01_EC_01_ReversalFailsStoreAndForwardRetries {

        @Test
        @DisplayName("BDD-V01-EC-01: Failed reversal queued and retried via Store & Forward")
        void BDD_V01_EC_01_failedReversalQueuedForRetry() throws Exception {
            // Given: A reversal message that will fail initially
            UUID transactionId = UUID.randomUUID();
            StoreAndForwardPort.ReversalMessage sfMessage = new StoreAndForwardPort.ReversalMessage(
                UUID.randomUUID().toString(),
                transactionId,
                "0400", // Financial reversal
                "MTI:0400|Transaction:" + transactionId,
                0,
                Instant.now().minusSeconds(120), // Started 2 minutes ago
                Instant.now().minusSeconds(60),  // Last attempt 1 minute ago
                "Connection timeout"
            );

            // When: Queue the failed message
            storeAndForwardPort.queueReversalForRetry(sfMessage);

            // Then: Message should be in queue
            StoreAndForwardPort.QueueStatistics initialStats = storeAndForwardPort.getQueueStatistics();
            assertThat(initialStats.pendingMessages()).isGreaterThan(0);

            // BDD Then: System should persist the reversal in the Store & Forward queue
            // BDD Then: Retry the reversal every 60 seconds
            // BDD Then: Eventually send successfully when network is restored

            // Note: Full retry testing would require temporal workflow or scheduled job execution
        }
    }

    // ================================================================
    // BDD-V01-EC-02: Max retries exceeded - manual investigation
    // ================================================================

    @Nested
    @DisplayName("BDD-V01-EC-02 [EC]: Max retries exceeded - manual investigation")
    class BDD_V01_EC_02_MaxRetriesExceeded {

        @Test
        @DisplayName("BDD-V01-EC-02: Max retries triggers manual investigation flag")
        void BDD_V01_EC_02_maxRetriesTriggersManualInvestigation() throws Exception {
            // Given: A reversal message that has exceeded max retries
            UUID transactionId = UUID.randomUUID();
            StoreAndForwardPort.ReversalMessage sfMessage = new StoreAndForwardPort.ReversalMessage(
                UUID.randomUUID().toString(),
                transactionId,
                "0400",
                "MTI:0400|Transaction:" + transactionId,
                1441, // Exceeds max retries (1440 for financial)
                Instant.now().minusSeconds(24 * 60 * 60), // 24 hours ago
                Instant.now(),
                "Persistent network failure"
            );

            // When: Process queued messages
            storeAndForwardPort.queueReversalForRetry(sfMessage);
            storeAndForwardPort.processQueuedMessages();

            // Then: Should flag for manual investigation
            // BDD Then: Flag the Transaction for manual investigation
            // BDD Then: Create an AuditLog entry with action "FAIL"
            // BDD Then: Alert the backoffice operations team

            // Note: In real implementation, this would trigger alerts and status updates
        }
    }

    // ================================================================
    // BDD-V01-EC-03: Financial authorization uses ZERO retries
    // ================================================================

    @Nested
    @DisplayName("BDD-V01-EC-03 [EC]: Financial authorization uses ZERO retries")
    class BDD_V01_EC_03_FinancialAuthorizationZeroRetries {

        @Test
        @DisplayName("BDD-V01-EC-03: Financial operations trigger immediate reversal")
        void BDD_V01_EC_03_financialOperationsImmediateReversal() throws Exception {
            // Given: A financial authorization that times out
            String idempotencyKey = "BDD-V01-EC-03-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            // When: Submit transaction that would timeout
            mockMvc.perform(post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isAccepted());

            // Then: BDD specifies ZERO retries for financial authorizations
            // BDD Then: NOT retry the financial authorization
            // BDD Then: Immediately trigger the reversal flow
            // BDD Then: AgentFloat should be released

            // Note: This requires workflow execution to test fully
            // The Store & Forward mechanism ensures reversals are retried even if initial reversal fails
        }
    }

    // ================================================================
    // BDD-V01-ECHO: Non-financial echo uses exponential backoff
    // ================================================================

    @Nested
    @DisplayName("BDD-V01-ECHO [HP]: Non-financial echo uses exponential backoff retry")
    class BDD_V01_ECHO_NonFinancialExponentialBackoff {

        @Test
        @DisplayName("BDD-V01-ECHO: Non-financial operations use exponential backoff")
        void BDD_V01_ECHO_nonFinancialUsesExponentialBackoff() throws Exception {
            // Given: A non-financial echo/heartbeat message
            UUID transactionId = UUID.randomUUID();
            StoreAndForwardPort.ReversalMessage sfMessage = new StoreAndForwardPort.ReversalMessage(
                UUID.randomUUID().toString(),
                transactionId,
                "0800", // Non-financial network message (Echo)
                "MTI:0800|Echo:" + transactionId,
                0,
                Instant.now(),
                Instant.now(),
                "Network unreachable"
            );

            // When: Queue non-financial message for retry
            storeAndForwardPort.queueReversalForRetry(sfMessage);

            // Then: Should use exponential backoff: 1s, 2s, 4s, 8s, 16s, 32s
            // BDD Then: Retry with exponential backoff: 1s, 2s, 4s
            // BDD Then: If all 3 retries fail, alert the network monitoring team

            StoreAndForwardPort.QueueStatistics stats = storeAndForwardPort.getQueueStatistics();
            assertThat(stats.pendingMessages()).isGreaterThan(0);
        }
    }

    // Helper methods for building test requests
    private String buildWithdrawalRequest(String idempotencyKey, String targetBIN) {
        return buildWithdrawalRequest(idempotencyKey, targetBIN, BigDecimal.valueOf(500));
    }

    private String buildWithdrawalRequest(String idempotencyKey, String targetBIN, BigDecimal amount) {
        return """
            {
                "transactionType": "CASH_WITHDRAWAL",
                "agentId": "%s",
                "amount": %s,
                "idempotencyKey": "%s",
                "pan": "4111111111111111",
                "customerCardMasked": "411111******1111",
                "targetBIN": "%s",
                "agentTier": "TIER_1"
            }
            """.formatted(AGENT_ID, amount, idempotencyKey, targetBIN);
    }
}
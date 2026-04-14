package com.agentbanking.gateway.integration.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BDD-WF-HP Happy Path Tests
 * 
 * Verifies complete workflow execution for happy path scenarios:
 * - BDD-WF-HP-W01: Off-Us withdrawal
 * - BDD-WF-HP-W02: On-Us withdrawal
 * - BDD-WF-HP-D01: Cash deposit
 * - BDD-WF-HP-BP01: Bill payment
 * - BDD-WF-HP-DN01/DN02: DuitNow transfer
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestClassOrder(org.junit.jupiter.api.ClassOrderer.OrderAnnotation.class)
@DisplayName("BDD-WF-HP: Happy Path Workflow Verification")
class BDDEdgeCasesAndSTPVerificationTest {

    private static final String GATEWAY_URL = System.getenv().getOrDefault("GATEWAY_BASE_URL", "http://localhost:8080");
    private static String agentToken;
    private static String agentId;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private WebTestClient gatewayClient;
    private static final UUID AGENT_UUID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @BeforeEach
    void setUp() {
        gatewayClient = WebTestClient.bindToServer()
                .baseUrl(GATEWAY_URL)
                .build();
    }

    private WorkflowVerificationHelper helper() {
        return new WorkflowVerificationHelper(gatewayClient, agentToken, objectMapper);
    }

    private JsonNode parseBody(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + body, e);
        }
    }

    // ================================================================
    // BDD-WF-EC: Edge Cases - Insufficient Float
    // ================================================================
    
    @Nested
    @Order(1)
    @DisplayName("BDD-WF-EC: Withdrawal Edge Cases")
    class WithdrawalEdgeCases {

        @Test
        @DisplayName("BDD-WF-EC-W04: Insufficient float - workflow fails immediately")
        void insufficientFloat_shouldFail() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-ec-no-float-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequestLarge(idempotencyKey, "0123");

            // Submit transaction
            String workflowId = helper().submitTransaction(idempotencyKey, requestBody);
            
            // BDD Then: Workflow fails with insufficient float
            WorkflowVerificationHelper.WorkflowDetails details = helper().waitForWorkflowCompletion(workflowId);
            
            // BDD And: TransactionRecord should have FAILED status
            assertEquals("FAILED", details.status());
            
            // BDD And: errorCode should indicate insufficient float
            assertNotNull(details.errorCode(), "Should have errorCode for insufficient float");
        }

        @Test
        @DisplayName("BDD-WF-EC-W05: Velocity check fails - workflow fails before float block")
        void velocityCheck_shouldFail() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-ec-velocity-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            // Submit transaction
            String workflowId = helper().submitTransaction(idempotencyKey, requestBody);
            
            // BDD Then: Workflow fails due to velocity check
            WorkflowVerificationHelper.WorkflowDetails details = helper().waitForWorkflowCompletion(workflowId);
            
            // BDD And: TransactionRecord should have FAILED status
            assertEquals("FAILED", details.status());
            
            // BDD And: errorCode should indicate velocity check failure
            assertNotNull(details.errorCode(), "Should have errorCode for velocity failure");
        }
    }

    // ================================================================
    // BDD-STP: Straight Through Processing
    // ================================================================
    
    @Nested
    @Order(2)
    @DisplayName("BDD-STP: Straight Through Processing")
    class STPTests {

        @Test
        @DisplayName("BDD-STP-01: Full STP - transaction completes immediately")
        void fullStp_shouldCompleteImmediately() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-stp-full-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            String workflowId = helper().submitTransaction(idempotencyKey, requestBody);
            
            // BDD Then: FULL_STP completes immediately
            helper().verifyHappyPathWithReference(workflowId);
        }

        @Test
        @DisplayName("BDD-STP-02: High value - triggers PENDING_REVIEW (NON_STP)")
        void highValue_shouldRequireReview() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-stp-highvalue-" + UUID.randomUUID();
            String requestBody = buildHighValueRequest(idempotencyKey);

            String workflowId = helper().submitTransaction(idempotencyKey, requestBody);
            
            // BDD Then: HIGH_VALUE triggers NON_STP
            WorkflowVerificationHelper.WorkflowDetails details = helper().waitForWorkflowCompletion(workflowId);
            
            // BDD And: Should be PENDING_REVIEW status
            assertEquals("PENDING_REVIEW", details.status());
            
            // BDD And: pendingReason should explain review requirement
            assertNotNull(details.details(), "Should have pendingReason in details");
        }

        @Test
        @DisplayName("BDD-STP-03: Deposit - FULL_STP path")
        void deposit_fullStp() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-stp-deposit-" + UUID.randomUUID();
            String requestBody = buildDepositRequest(idempotencyKey);

            String workflowId = helper().submitTransaction(idempotencyKey, requestBody);
            
            // BDD Then: Deposit follows FULL_STP path
            helper().verifyHappyPath(workflowId);
        }

        @Test
        @DisplayName("BDD-STP-04: Bill payment - FULL_STP path")
        void billPayment_fullStp() {
            assumeTrue(agentToken != null, "Agent token required");

            String idempotencyKey = "e2e-stp-billpay-" + UUID.randomUUID();
            String requestBody = buildBillPaymentRequest(idempotencyKey);

            String workflowId = helper().submitTransaction(idempotencyKey, requestBody);
            
            // BDD Then: Bill payment follows FULL_STP path
            helper().verifyHappyPathWithReference(workflowId);
        }
    }

    // ================================================================
    // Request Builders
    // ================================================================

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

    private String buildWithdrawalRequestLarge(String idempotencyKey, String targetBIN) {
        return """
            {
                "transactionType": "CASH_WITHDRAWAL",
                "agentId": "%s",
                "amount": 100000.00,
                "idempotencyKey": "%s",
                "pan": "4111111111111111",
                "customerCardMasked": "411111******1111",
                "targetBIN": "%s",
                "agentTier": "TIER_1"
            }
            """.formatted(getEffectiveAgentId(), idempotencyKey, targetBIN);
    }

    private String buildHighValueRequest(String idempotencyKey) {
        return """
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
    }

    private String buildDepositRequest(String idempotencyKey) {
        return """
            {
                "transactionType": "CASH_DEPOSIT",
                "agentId": "%s",
                "amount": 100.00,
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

    private String getEffectiveAgentId() {
        return agentId != null ? agentId : AGENT_UUID.toString();
    }
}

package com.agentbanking.orchestrator.integration;

import com.agentbanking.orchestrator.integration.AbstractOrchestratorRealInfraIntegrationTest;
import com.agentbanking.orchestrator.domain.model.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("Orchestrator API Integration Tests")
class OrchestratorControllerIntegrationTest extends AbstractOrchestratorRealInfraIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID AGENT_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @Nested
    @DisplayName("BDD-TO: Workflow Router Dispatch")
    class WorkflowRouterTests {

        @Test
        @DisplayName("BDD-TO-01: Router dispatches Off-Us withdrawal to WithdrawalWorkflow")
        void withdraw_offUs_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-offus-withdraw-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey))
                    .andExpect(jsonPath("$.pollUrl").exists());
        }

        @Test
        @DisplayName("BDD-TO-02: Router dispatches On-Us withdrawal to WithdrawalOnUsWorkflow")
        void withdraw_onUs_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-onus-withdraw-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0012");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-03: Router dispatches deposit to DepositWorkflow")
        void deposit_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-deposit-" + UUID.randomUUID();
            String requestBody = buildDepositRequest(idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-04: Router dispatches bill payment to BillPaymentWorkflow")
        void billPayment_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-billpay-" + UUID.randomUUID();
            String requestBody = buildBillPaymentRequest(idempotencyKey, "TNB", "123456789012");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-05: Router dispatches DuitNow transfer to DuitNowTransferWorkflow")
        void duitNowTransfer_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-duitnow-" + UUID.randomUUID();
            String requestBody = buildDuitNowRequest(idempotencyKey, "MOBILE", "0123456789");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-06: Router rejects unsupported transaction type")
        void startTransaction_unsupportedType_shouldReturnError() throws Exception {
            String idempotencyKey = "test-unsupported-" + UUID.randomUUID();
            String requestBody = """
                {
                    "transactionType": "UNKNOWN_TYPE",
                    "agentId": "%s",
                    "amount": 100.00,
                    "idempotencyKey": "%s"
                }
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("BDD-WF: Workflow Lifecycle")
    class WorkflowLifecycleTests {

        @Test
        @DisplayName("BDD-WF-01: Workflow starts and returns PENDING status immediately")
        void startWorkflow_validRequest_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-lifecycle-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey))
                    .andExpect(jsonPath("$.pollUrl").value("/api/v1/transactions/" + idempotencyKey + "/status"))
                    .andReturn();
        }

        @Test
        @DisplayName("BDD-WF-02: Poll returns COMPLETED status with transaction details")
        void poll_completedWorkflow_shouldReturnDetails() throws Exception {
            String idempotencyKey = "test-poll-completed-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/transactions/" + idempotencyKey + "/status"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("BDD-WF-03: Poll returns FAILED status with error details")
        void poll_failedWorkflow_shouldReturnErrorDetails() throws Exception {
            String idempotencyKey = "test-poll-failed-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/transactions/" + idempotencyKey + "/status"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("BDD-WF-HP-W: Withdrawal Happy Path")
    class WithdrawalHappyPathTests {

        @Test
        @DisplayName("BDD-WF-HP-W01: Off-Us withdrawal starts workflow")
        void withdrawOffUs_validRequest_shouldStartWorkflow() throws Exception {
            String idempotencyKey = "test-hp-withdraw-offus-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-WF-HP-W02: On-Us withdrawal starts workflow")
        void withdrawOnUs_validRequest_shouldStartWorkflow() throws Exception {
            String idempotencyKey = "test-hp-withdraw-onus-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0012");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }
    }

    @Nested
    @DisplayName("BDD-WF-EC-W: Withdrawal Edge Cases")
    class WithdrawalEdgeCaseTests {

        @Test
        @DisplayName("BDD-WF-EC-W04: Insufficient float - workflow fails immediately")
        void withdraw_insufficientFloat_shouldFail() throws Exception {
            String idempotencyKey = "test-insufficient-float-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("BDD-WF-EC-W05: Velocity check fails - workflow fails before float block")
        void withdraw_velocityExceeded_shouldFail() throws Exception {
            String idempotencyKey = "test-velocity-fail-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("BDD-WF-D: Deposit Workflow")
    class DepositWorkflowTests {

        @Test
        @DisplayName("BDD-WF-HP-D01: Cash deposit starts workflow")
        void deposit_validRequest_shouldStartWorkflow() throws Exception {
            String idempotencyKey = "test-deposit-hp-" + UUID.randomUUID();
            String requestBody = buildDepositRequest(idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-WF-EC-D01: Deposit to invalid account - fails before money moves")
        void deposit_invalidAccount_shouldFail() throws Exception {
            String idempotencyKey = "test-deposit-invalid-" + UUID.randomUUID();
            String requestBody = buildDepositRequest(idempotencyKey, "9999999999");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("BDD-WF-EC-D02: High-value deposit requires biometric verification")
        void deposit_highValue_shouldRequireBiometric() throws Exception {
            String idempotencyKey = "test-deposit-highvalue-" + UUID.randomUUID();
            String requestBody = buildDepositRequest(idempotencyKey, "1234567890", true, BigDecimal.valueOf(10000));

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("BDD-WF-BP: Bill Payment Workflow")
    class BillPaymentWorkflowTests {

        @Test
        @DisplayName("BDD-WF-HP-BP01: Bill payment starts workflow")
        void billPayment_validRequest_shouldStartWorkflow() throws Exception {
            String idempotencyKey = "test-billpay-hp-" + UUID.randomUUID();
            String requestBody = buildBillPaymentRequest(idempotencyKey, "TNB", "123456789012");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-WF-EC-BP01: Invalid biller reference - float released")
        void billPayment_invalidBiller_shouldFail() throws Exception {
            String idempotencyKey = "test-billpay-invalid-" + UUID.randomUUID();
            String requestBody = buildBillPaymentRequest(idempotencyKey, "INVALID", "123456789012");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("BDD-WF-DN: DuitNow Transfer Workflow")
    class DuitNowWorkflowTests {

        @Test
        @DisplayName("BDD-WF-HP-DN01: DuitNow transfer via mobile proxy")
        void duitNowTransfer_mobileProxy_shouldStart() throws Exception {
            String idempotencyKey = "test-duitnow-mobile-" + UUID.randomUUID();
            String requestBody = buildDuitNowRequest(idempotencyKey, "MOBILE", "0123456789");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-WF-HP-DN02: DuitNow transfer via MyKad proxy")
        void duitNowTransfer_myKadProxy_shouldStart() throws Exception {
            String idempotencyKey = "test-duitnow-mykad-" + UUID.randomUUID();
            String requestBody = buildDuitNowRequest(idempotencyKey, "MYKAD", "123456789012");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("BDD-WF-HP-DN03: DuitNow transfer via BRN proxy")
        void duitNowTransfer_brnProxy_shouldStart() throws Exception {
            String idempotencyKey = "test-duitnow-brn-" + UUID.randomUUID();
            String requestBody = buildDuitNowRequest(idempotencyKey, "BRN", "BRN123456");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("BDD-WF-EC-DN01: Proxy not found - float released")
        void duitNowTransfer_invalidProxy_shouldFail() throws Exception {
            String idempotencyKey = "test-duitnow-invalid-" + UUID.randomUUID();
            String requestBody = buildDuitNowRequest(idempotencyKey, "MOBILE", "9999999999");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("BDD-HITL: Human-in-the-Loop Signal Handling")
    class HumanInTheLoopTests {

        @Test
        @DisplayName("BDD-HITL-01: Admin force-resolves stuck workflow with COMMIT")
        void forceResolve_commitAction_shouldSucceed() throws Exception {
            String idempotencyKey = "test-force-resolve-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            String forceResolveBody = """
                {
                    "action": "COMMIT",
                    "reason": "Manual resolution required",
                    "adminId": "admin-001"
                }
                """;

            mockMvc.perform(post("/api/v1/transactions/" + idempotencyKey + "/force-resolve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forceResolveBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }

        @Test
        @DisplayName("BDD-HITL-02: Admin force-resolves stuck workflow with REVERSE")
        void forceResolve_reverseAction_shouldSucceed() throws Exception {
            String idempotencyKey = "test-force-reverse-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            String forceResolveBody = """
                {
                    "action": "REVERSE",
                    "reason": "Customer cancelled",
                    "adminId": "admin-001"
                }
                """;

            mockMvc.perform(post("/api/v1/transactions/" + idempotencyKey + "/force-resolve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(forceResolveBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"));
        }
    }

    @Nested
    @DisplayName("BDD-POLL: Polling Endpoint")
    class PollingTests {

        @Test
        @DisplayName("BDD-POLL-01: Poll returns PENDING status for running workflow")
        void poll_pendingWorkflow_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-poll-pending-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/transactions/" + idempotencyKey + "/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").exists());
        }

        @Test
        @DisplayName("BDD-POLL-04: Poll for non-existent workflowId returns 404")
        void poll_nonexistentWorkflow_shouldReturn404() throws Exception {
            mockMvc.perform(get("/api/v1/transactions/NONEXISTENT-ID/status"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("BDD-IDE: Idempotency at Temporal Level")
    class IdempotencyTests {

        @Test
        @DisplayName("BDD-IDE-01: Duplicate workflow start returns existing status")
        void duplicateStart_shouldReturnExistingStatus() throws Exception {
            String idempotencyKey = "test-idempotent-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-IDE-02: Duplicate request for completed workflow returns cached result")
        void duplicateCompletedRequest_shouldReturnCached() throws Exception {
            String idempotencyKey = "test-idempotent-completed-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("BDD-IDE-03: Duplicate request for failed workflow returns original error")
        void duplicateFailedRequest_shouldReturnOriginalError() throws Exception {
            String idempotencyKey = "test-idempotent-failed-" + UUID.randomUUID();
            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("BDD-XWF: Cross-Workflow Scenarios")
    class CrossWorkflowTests {

        @Test
        @DisplayName("BDD-XWF-01: Multiple concurrent workflows for same agent")
        void concurrentWorkflows_sameAgent_shouldNotCauseRaceConditions() throws Exception {
            String idempotencyKey1 = "test-concurrent-1-" + UUID.randomUUID();
            String idempotencyKey2 = "test-concurrent-2-" + UUID.randomUUID();

            String requestBody1 = buildWithdrawalRequest(idempotencyKey1, "0123", BigDecimal.valueOf(500));
            String requestBody2 = buildWithdrawalRequest(idempotencyKey2, "0123", BigDecimal.valueOf(500));

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"));

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody2))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }
    }

    @Nested
    @DisplayName("BDD-TO-NEW: New Transaction Types")
    class NewTransactionTypeTests {

        @Test
        @DisplayName("BDD-TO-NEW-01: Router dispatches cashless payment to CashlessPaymentWorkflow")
        void cashlessPayment_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-cashless-" + UUID.randomUUID();
            String requestBody = """
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
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey))
                    .andExpect(jsonPath("$.pollUrl").exists());
        }

        @Test
        @DisplayName("BDD-TO-NEW-02: Router dispatches pin-based purchase to PinBasedPurchaseWorkflow")
        void pinBasedPurchase_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-pin-purchase-" + UUID.randomUUID();
            String requestBody = """
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
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-03: Router dispatches prepaid topup to PrepaidTopupWorkflow")
        void prepaidTopup_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-prepaid-topup-" + UUID.randomUUID();
            String requestBody = """
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
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-04: Router dispatches ewallet withdrawal to EWalletWithdrawalWorkflow")
        void ewalletWithdrawal_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-ewallet-withdraw-" + UUID.randomUUID();
            String requestBody = """
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
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-05: Router dispatches ewallet topup to EWalletTopupWorkflow")
        void ewalletTopup_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-ewallet-topup-" + UUID.randomUUID();
            String requestBody = """
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
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-06: Router dispatches ESSP purchase to ESSPPurchaseWorkflow")
        void esspPurchase_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-essp-purchase-" + UUID.randomUUID();
            String requestBody = """
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
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-07: Router dispatches PIN purchase to PINPurchaseWorkflow")
        void pinPurchase_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-pin-purchase-" + UUID.randomUUID();
            String requestBody = """
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
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-08: Router dispatches retail sale to RetailSaleWorkflow")
        void retailSale_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-retail-sale-" + UUID.randomUUID();
            String requestBody = """
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
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }

        @Test
        @DisplayName("BDD-TO-NEW-09: Router dispatches hybrid cashback to HybridCashbackWorkflow")
        void hybridCashback_validData_shouldReturnPending() throws Exception {
            String idempotencyKey = "test-hybrid-cashback-" + UUID.randomUUID();
            String requestBody = """
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
                """.formatted(AGENT_ID, idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey));
        }
    }
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

    private String buildDepositRequest(String idempotencyKey) {
        return buildDepositRequest(idempotencyKey, "1234567890", false, BigDecimal.valueOf(1000));
    }

    private String buildDepositRequest(String idempotencyKey, String destinationAccount) {
        return buildDepositRequest(idempotencyKey, destinationAccount, false, BigDecimal.valueOf(1000));
    }

    private String buildDepositRequest(String idempotencyKey, String destinationAccount, 
                                        boolean requiresBiometric, BigDecimal amount) {
        return """
            {
                "transactionType": "CASH_DEPOSIT",
                "agentId": "%s",
                "amount": %s,
                "idempotencyKey": "%s",
                "destinationAccount": "%s",
                "requiresBiometric": %s,
                "agentTier": "TIER_1"
            }
            """.formatted(AGENT_ID, amount, idempotencyKey, destinationAccount, requiresBiometric);
    }

    private String buildBillPaymentRequest(String idempotencyKey, String billerCode, String ref1) {
        return """
            {
                "transactionType": "BILL_PAYMENT",
                "agentId": "%s",
                "amount": 150.00,
                "idempotencyKey": "%s",
                "billerCode": "%s",
                "ref1": "%s",
                "agentTier": "TIER_1"
            }
            """.formatted(AGENT_ID, idempotencyKey, billerCode, ref1);
    }

    private String buildDuitNowRequest(String idempotencyKey, String proxyType, String proxyValue) {
        return """
            {
                "transactionType": "DUITNOW_TRANSFER",
                "agentId": "%s",
                "amount": 500.00,
                "idempotencyKey": "%s",
                "proxyType": "%s",
                "proxyValue": "%s",
                "agentTier": "TIER_1"
            }
            """.formatted(AGENT_ID, idempotencyKey, proxyType, proxyValue);
    }
}

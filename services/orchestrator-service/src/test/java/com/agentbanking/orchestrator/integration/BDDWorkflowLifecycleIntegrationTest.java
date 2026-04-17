package com.agentbanking.orchestrator.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@DisplayName("BDD-WF Series: Workflow Lifecycle")
class BDDWorkflowLifecycleIntegrationTest extends AbstractOrchestratorRealInfraIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final UUID AGENT_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    @Nested
    @DisplayName("BDD-WF-02 [HP]: Workflow completes successfully")
    class BDD_WF_02_WorkflowCompletesSuccessfully {

        @Test
        @DisplayName("BDD-WF-02: Transaction submitted and workflow started successfully")
        void BDD_WF_02_workflowStartedSuccessfully() throws Exception {
            String idempotencyKey = "BDD-WF-02-" + UUID.randomUUID();

            String requestBody = buildWithdrawalRequest(idempotencyKey, "0123");

            MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                            .contentType("application/json")
                            .content(requestBody))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.workflowId").value(idempotencyKey))
                    .andExpect(jsonPath("$.pollUrl").exists())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            assertThat(response.get("workflowId").asText()).isEqualTo(idempotencyKey);
        }
    }

    @Nested
    @DisplayName("BDD-WF-03 [HP]: Workflow failure handling")
    class BDD_WF_03_WorkflowFailsAndRecordsError {

        @Test
        @DisplayName("BDD-WF-03: Invalid transaction with missing required field rejected")
        void BDD_WF_03_invalidTransactionRejected() throws Exception {
            String idempotencyKey = "BDD-WF-03-" + UUID.randomUUID();

            String invalidRequest = """
                {
                    "transactionType": "CASH_WITHDRAWAL",
                    "idempotencyKey": "%s"
                }
                """.formatted(idempotencyKey);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType("application/json")
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    private String buildWithdrawalRequest(String idempotencyKey, String targetBIN) {
        return """
            {
                "transactionType": "CASH_WITHDRAWAL",
                "agentId": "%s",
                "amount": 500.00,
                "idempotencyKey": "%s",
                "pan": "4111111111111111",
                "customerCardMasked": "411111******1111",
                "targetBIN": "%s",
                "agentTier": "TIER_1"
            }
            """.formatted(AGENT_ID, idempotencyKey, targetBIN);
    }
}

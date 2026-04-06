package com.agentbanking.orchestrator.integration;

import com.agentbanking.common.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class OrchestratorControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void withdraw_withValidData_shouldReturnPending() throws Exception {
        String requestBody = """
            {
                "transactionType": "CASH_WITHDRAWAL",
                "agentId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "amount": 500.00,
                "idempotencyKey": "test-withdraw-001",
                "pan": "4111111111111111",
                "customerCardMasked": "411111******1111",
                "geofenceLat": 3.1390,
                "geofenceLng": 101.6869,
                "agentTier": "TIER_1"
            }
            """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.workflowId").value("test-withdraw-001"))
                .andExpect(jsonPath("$.pollUrl").exists());
    }

    @Test
    void getStatus_withUnknownWorkflow_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/nonexistent-workflow/status"))
                .andExpect(status().isNotFound());
    }

    @Test
    void billPayment_withValidData_shouldReturnPending() throws Exception {
        String requestBody = """
            {
                "transactionType": "BILL_PAYMENT",
                "agentId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "amount": 150.00,
                "idempotencyKey": "test-billpay-001",
                "billerCode": "MAXIS",
                "ref1": "0123456789",
                "agentTier": "TIER_1"
            }
            """;

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.workflowId").value("test-billpay-001"));
    }
}
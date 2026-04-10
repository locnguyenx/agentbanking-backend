package com.agentbanking.switchadapter.integration;

import com.agentbanking.common.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class SwitchControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cardAuth_withValidData_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "internalTransactionId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "pan": "4111111111111111",
                "amount": 100.00
            }
            """;

        mockMvc.perform(post("/internal/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void reversal_withValidData_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "originalTransactionId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "originalReference": "REF123",
                "amount": 100.00
            }
            """;

        mockMvc.perform(post("/internal/reversal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void duitNowTransfer_withValidData_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "internalTransactionId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "proxyType": "PHONE",
                "proxyValue": "0123456789",
                "amount": 50.00
            }
            """;

        mockMvc.perform(post("/internal/duitnow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void balanceInquiry_withValidData_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "encryptedCardData": "encrypted123",
                "pinBlock": "pin123"
            }
            """;

        mockMvc.perform(post("/internal/balance-inquiry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void cardAuth_withInvalidPan_shouldReturn400() throws Exception {
        String requestBody = """
            {
                "internalTransactionId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "pan": "123",
                "amount": 100.00
            }
            """;

        mockMvc.perform(post("/internal/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void cardAuth_withMissingAmount_shouldReturn400() throws Exception {
        String requestBody = """
            {
                "internalTransactionId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "pan": "4111111111111111"
            }
            """;

        mockMvc.perform(post("/internal/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void duitNowTransfer_withInvalidProxyValue_shouldReturnError() throws Exception {
        String requestBody = """
            {
                "internalTransactionId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "proxyType": "PHONE",
                "proxyValue": "",
                "amount": 50.00
            }
            """;

        mockMvc.perform(post("/internal/duitnow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
}

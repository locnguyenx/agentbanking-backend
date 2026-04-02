package com.agentbanking.biller.integration;

import com.agentbanking.common.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class BillerControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validateRef_withValidData_shouldReturnValidation() throws Exception {
        String requestBody = """
            {
                "billerCode": "TEST123",
                "ref1": "1234567890"
            }
            """;

        mockMvc.perform(post("/internal/validate-ref")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").exists())
                .andExpect(jsonPath("$.billerCode").exists());
    }

    @Test
    void payBill_withValidData_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "billerCode": "TEST123",
                "ref1": "1234567890",
                "amount": "100.00",
                "internalTransactionId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
            }
            """;

        mockMvc.perform(post("/internal/pay-bill")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$").exists());
    }

    @Test
    void topup_withValidData_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "telco": "CELCOM",
                "phoneNumber": "0123456789",
                "amount": "50.00"
            }
            """;

        mockMvc.perform(post("/internal/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.topupId").exists());
    }

    @Test
    void jomPay_withValidData_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "billerCode": "TEST123",
                "billerName": "Test Biller",
                "ref1": "1234567890",
                "ref2": "",
                "amount": "100.00",
                "currency": "MYR"
            }
            """;

        mockMvc.perform(post("/internal/billpayment/jompay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$").exists());
    }
}

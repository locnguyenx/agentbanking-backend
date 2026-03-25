package com.agentbanking.mock.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaynetMockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldApproveCardAuthByDefault() throws Exception {
        mockMvc.perform(post("/mock/paynet/iso8583/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pan\":\"4111111111111111\",\"amount\":500.00,\"merchantCode\":\"AGT-001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.responseCode").value("00"))
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.referenceId").isNotEmpty());
    }

    @Test
    void shouldAcknowledgeReversal() throws Exception {
        mockMvc.perform(post("/mock/paynet/iso8583/reversal")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalReferenceId\":\"REF-123\",\"amount\":500.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    void shouldApproveDuitNowTransfer() throws Exception {
        mockMvc.perform(post("/mock/paynet/iso20022/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proxyType\":\"MOBILE\",\"proxyValue\":\"0123456789\",\"amount\":1000.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SETTLED"));
    }
}
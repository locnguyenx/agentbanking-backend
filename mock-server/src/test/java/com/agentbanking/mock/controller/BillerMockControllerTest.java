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
class BillerMockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldValidateJompayRef() throws Exception {
        mockMvc.perform(post("/mock/billers/JOMPAY/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ref1\":\"INV-12345\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.amount").isNumber());
    }

    @Test
    void shouldRejectInvalidRef() throws Exception {
        mockMvc.perform(post("/mock/billers/JOMPAY/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ref1\":\"INVALID-REF\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void shouldApproveBillerPayment() throws Exception {
        mockMvc.perform(post("/mock/billers/JOMPAY/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ref1\":\"INV-12345\",\"amount\":150.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));
    }
}

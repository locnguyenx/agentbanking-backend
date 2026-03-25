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
class JpnMockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnCitizenDataForValidMykad() throws Exception {
        mockMvc.perform(post("/mock/jpn/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mykad\":\"123456789012\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("AHMAD BIN ABU"))
            .andExpect(jsonPath("$.amlStatus").value("CLEAN"))
            .andExpect(jsonPath("$.age").isNumber());
    }

    @Test
    void shouldReturnNotFoundForUnknownMykad() throws Exception {
        mockMvc.perform(post("/mock/jpn/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mykad\":\"000000000000\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void shouldReturnMatchForBiometric() throws Exception {
        mockMvc.perform(post("/mock/jpn/biometric")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"verificationId\":\"KYC-001\",\"biometricData\":\"blob\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.match").value("MATCH"));
    }
}

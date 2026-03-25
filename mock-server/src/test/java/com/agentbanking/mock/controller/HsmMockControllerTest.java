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
class HsmMockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnValidPinVerification() throws Exception {
        mockMvc.perform(post("/mock/hsm/verify-pin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pinBlock\":\"encrypted-pin-blob\",\"pan\":\"4111111111111111\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));
    }
}

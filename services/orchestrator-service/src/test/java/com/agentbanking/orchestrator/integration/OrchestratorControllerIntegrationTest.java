package com.agentbanking.orchestrator.integration;

import com.agentbanking.orchestrator.domain.port.out.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrchestratorControllerIntegrationTest {

    @MockBean
    private IdempotencyService idempotencyService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void withdraw_withValidData_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "agentId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "amount": 500.00,
                "pan": "4111111111111111",
                "customerCardMasked": "411111******1111",
                "geofenceLat": 3.1390,
                "geofenceLng": 101.6869
            }
            """;

        mockMvc.perform(post("/api/v1/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$").exists());
    }
}
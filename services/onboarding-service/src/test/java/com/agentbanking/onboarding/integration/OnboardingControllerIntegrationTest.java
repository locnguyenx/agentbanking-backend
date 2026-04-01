package com.agentbanking.onboarding.integration;

import com.agentbanking.onboarding.infrastructure.external.AmlScreeningFeignClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnboardingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AmlScreeningFeignClient amlScreeningFeignClient;

    private void stubAmlScreening() {
        try {
            when(amlScreeningFeignClient.screen(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                    .thenReturn(java.util.Map.of("passed", true));
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    void verifyMyKad_withValidMyKad_shouldReturnVerification() throws Exception {
        String requestBody = """
            {
                "mykadNumber": "880101011234"
            }
            """;

        mockMvc.perform(post("/internal/verify-mykad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationId").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void verifyMyKad_withInvalidMyKad_shouldReturn400() throws Exception {
        String requestBody = """
            {
                "mykadNumber": "INVALID"
            }
            """;

        mockMvc.perform(post("/internal/verify-mykad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void biometricMatch_withValidData_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "verificationId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
                "biometricData": "base64encodeddata"
            }
            """;

        mockMvc.perform(post("/internal/biometric")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationId").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void getKycReviewQueue_shouldReturnQueue() throws Exception {
        mockMvc.perform(get("/internal/kyc/review-queue")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    void submitApplication_withValidData_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "mykadNumber": "880101011234",
                "extractedName": "TEST USER",
                "ssmBusinessName": "TEST BUSINESS",
                "ssmOwnerName": "TEST OWNER",
                "agentTier": "STANDARD",
                "merchantGpsLat": "3.1390",
                "merchantGpsLng": "101.6869",
                "phoneNumber": "0123456789"
            }
            """;

        mockMvc.perform(post("/internal/onboarding/application")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.applicationId").exists())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void startMicroAgentOnboarding_withValidMyKad_shouldStart() throws Exception {
        stubAmlScreening();
        
        String requestBody = """
            {
                "mykadNumber": "880101011234"
            }
            """;

        mockMvc.perform(post("/internal/onboarding/agent/micro/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onboardingId").exists());
    }

    @Test
    void evaluateMicroAgentOnboarding_withNonExistentId_shouldReturnError() throws Exception {
        stubAmlScreening();
        
        String requestBody = """
            {
                "onboardingId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
            }
            """;

        mockMvc.perform(post("/internal/onboarding/agent/micro/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
}
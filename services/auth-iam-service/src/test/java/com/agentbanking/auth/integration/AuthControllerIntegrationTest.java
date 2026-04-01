package com.agentbanking.auth.integration;

import com.agentbanking.auth.application.usecase.AuthenticateUserUseCaseImpl;
import com.agentbanking.auth.domain.model.AuthenticationResult;
import com.agentbanking.auth.infrastructure.web.dto.AuthRequestDto;
import com.agentbanking.auth.infrastructure.web.dto.RefreshTokenDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for AuthController that tests actual HTTP endpoints
 * Uses existing PostgreSQL container on port 5439
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.yaml")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticateUserUseCaseImpl authenticateUserUseCase;

    private static final String VALID_USERNAME = "testuser";
    private static final String VALID_PASSWORD = "testpass123";
    private static final String ACCESS_TOKEN = "test-access-token-" + UUID.randomUUID();
    private static final String REFRESH_TOKEN = "test-refresh-token-" + UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Mock successful authentication
        AuthenticationResult authResult = new AuthenticationResult(
                ACCESS_TOKEN,
                REFRESH_TOKEN,
                900 // 15 minutes in seconds
        );

        when(authenticateUserUseCase.authenticate(anyString(), anyString()))
                .thenReturn(authResult);

        when(authenticateUserUseCase.refreshToken(anyString()))
                .thenReturn(authResult);
    }

    @Test
    void authenticate_withValidCredentials_shouldReturnTokens() throws Exception {
        AuthRequestDto request = new AuthRequestDto();
        request.setUsername(VALID_USERNAME);
        request.setPassword(VALID_PASSWORD);

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + VALID_USERNAME + "\",\"password\":\"" + VALID_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void authenticate_withInvalidCredentials_shouldReturnUnauthorized() throws Exception {
        // Override mock to return null for invalid credentials
        when(authenticateUserUseCase.authenticate("wronguser", "wrongpass"))
                .thenReturn(null);

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"wronguser\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.error.code").value("ERR_INVALID_CREDENTIALS"));
    }

    @Test
    void refreshToken_withValidToken_shouldReturnNewTokens() throws Exception {
        RefreshTokenDto request = new RefreshTokenDto(REFRESH_TOKEN);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + REFRESH_TOKEN + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(ACCESS_TOKEN))
                .andExpect(jsonPath("$.refreshToken").value(REFRESH_TOKEN))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void refreshToken_withInvalidToken_shouldReturnUnauthorized() throws Exception {
        // Override mock to return null for invalid refresh token
        when(authenticateUserUseCase.refreshToken("invalid-token"))
                .thenReturn(null);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"invalid-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.error.code").value("ERR_INVALID_REFRESH_TOKEN"));
    }
}
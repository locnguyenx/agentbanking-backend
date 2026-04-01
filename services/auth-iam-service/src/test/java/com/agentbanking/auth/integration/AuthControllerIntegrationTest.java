package com.agentbanking.auth.integration;

import com.agentbanking.common.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for AuthController that tests actual HTTP endpoints
 * Uses Testcontainers for PostgreSQL, Redis, and Kafka
 */
@AutoConfigureMockMvc
class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
                );
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticate_withValidCredentials_shouldReturnTokens() throws Exception {
        String requestBody = """
            {
                "username": "admin",
                "password": "password"
            }
            """;

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    void authenticate_withInvalidPassword_shouldReturn400() throws Exception {
        String requestBody = """
            {
                "username": "admin",
                "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ERR_VAL_INVALID_REQUEST"));
    }

    @Test
    void authenticate_withNonExistentUser_shouldReturn400() throws Exception {
        String requestBody = """
            {
                "username": "nonexistent",
                "password": "somepassword"
            }
            """;

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ERR_VAL_INVALID_REQUEST"));
    }

    @Test
    void refreshToken_withValidRefreshToken_shouldReturnNewTokens() throws Exception {
        // First, authenticate to get a refresh token
        String authRequestBody = """
            {
                "username": "admin",
                "password": "password"
            }
            """;

        MvcResult authResult = mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(authRequestBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = authResult.getResponse().getContentAsString();
        // Extract refresh token from response
        String refreshToken = responseBody.replaceAll(".*\"refreshToken\":\"([^\"]+)\".*", "$1");

        // Now use the refresh token to get new tokens
        String refreshRequestBody = """
            {
                "refreshToken": "%s"
            }
            """.formatted(refreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void refreshToken_withInvalidRefreshToken_shouldReturn401() throws Exception {
        String requestBody = """
            {
                "refreshToken": "invalid-refresh-token"
            }
            """;

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ERR_AUTH_UNAUTHORIZED"));
    }
}
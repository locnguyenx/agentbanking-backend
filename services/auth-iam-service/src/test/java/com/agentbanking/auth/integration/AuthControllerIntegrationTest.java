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
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for AuthController that tests actual HTTP endpoints
 * Uses Testcontainers for PostgreSQL, Redis, and Kafka
 */
@AutoConfigureMockMvc
@Import(AuthControllerIntegrationTest.TestConfig.class)
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
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.expires_in").exists());
    }

    @Test
    void authenticate_withInvalidPassword_shouldReturn401() throws Exception {
        String requestBody = """
            {
                "username": "admin",
                "password": "wrongpassword"
            }
            """;

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ERR_AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void authenticate_withNonExistentUser_shouldReturn401() throws Exception {
        String requestBody = """
            {
                "username": "nonexistent",
                "password": "somepassword"
            }
            """;

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ERR_AUTH_INVALID_CREDENTIALS"));
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
        // Extract refresh token from response (RFC 6749 format)
        String refreshToken = responseBody.replaceAll(".*\"refresh_token\":\"([^\"]+)\".*", "$1");

        // Now use the refresh token to get new tokens (RFC 6749 format)
        String refreshRequestBody = """
            {
                "refresh_token": "%s"
            }
            """.formatted(refreshToken);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists());
    }

    @Test
    void refreshToken_withInvalidRefreshToken_shouldReturn4xx() throws Exception {
        String requestBody = """
            {
                "refresh_token": "invalid-refresh-token"
            }
            """;

        // Invalid refresh token should return an error response (4xx)
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void authenticate_withMissingPassword_shouldReturn400() throws Exception {
        String requestBody = """
            {
                "username": "admin"
            }
            """;

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void authenticate_withShortPassword_shouldReturn400() throws Exception {
        String requestBody = """
            {
                "username": "admin",
                "password": "123"
            }
            """;

        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void authenticate_withEmptyBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }
}
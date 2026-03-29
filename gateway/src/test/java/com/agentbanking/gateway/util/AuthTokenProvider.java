package com.agentbanking.gateway.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for obtaining real JWT tokens from the auth system for E2E tests.
 * 
 * Configure in application-test.yaml:
 * - auth.token-endpoint: URL to get tokens
 * - auth.agent.*: POS terminal credentials
 * - auth.operator.*: Bank operator credentials  
 * - auth.admin.*: IT admin credentials
 * 
 * If auth service is unavailable, tests requiring auth will be skipped.
 */
public class AuthTokenProvider {

    private static final String TOKEN_ENDPOINT = System.getenv().getOrDefault(
            "AUTH_TOKEN_ENDPOINT", "https://auth.agentbanking.com/token");
    
    private static final WebClient webClient = WebClient.create(TOKEN_ENDPOINT);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, String> tokenCache = new ConcurrentHashMap<>();

    /**
     * Check if auth service is available
     */
    public static boolean isAvailable() {
        try {
            webClient.get()
                    .uri("/health")
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(5));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get token for POS terminal / Agent
     */
    public static String getAgentToken(String agentId, String tier) {
        String cacheKey = "agent:" + agentId + ":" + tier;
        return tokenCache.computeIfAbsent(cacheKey, k -> {
            String clientId = getEnvOrDefault("TEST_AGENT_CLIENT_ID", "pos-terminal-001");
            String clientSecret = getEnvOrDefault("TEST_AGENT_CLIENT_SECRET", "");
            String apiKey = getEnvOrDefault("TEST_AGENT_API_KEY", "");
            
            Map<String, String> body = Map.of(
                    "grant_type", "client_credentials",
                    "client_id", clientId,
                    "client_secret", clientSecret,
                    "scope", "agent:" + agentId + ":" + tier
            );
            
            return fetchToken(body);
        });
    }

    /**
     * Get token for Bank Operator (backoffice)
     */
    public static String getOperatorToken() {
        String cacheKey = "operator";
        return tokenCache.computeIfAbsent(cacheKey, k -> {
            String username = getEnvOrDefault("TEST_OPERATOR_USERNAME", "operator@agentbanking.com");
            String password = getEnvOrDefault("TEST_OPERATOR_PASSWORD", "");
            
            Map<String, String> body = Map.of(
                    "grant_type", "password",
                    "username", username,
                    "password", password,
                    "scope", "backoffice"
            );
            
            return fetchToken(body);
        });
    }

    /**
     * Get token for IT Admin
     */
    public static String getAdminToken() {
        String cacheKey = "admin";
        return tokenCache.computeIfAbsent(cacheKey, k -> {
            String username = getEnvOrDefault("TEST_ADMIN_USERNAME", "admin@agentbanking.com");
            String password = getEnvOrDefault("TEST_ADMIN_PASSWORD", "");
            
            Map<String, String> body = Map.of(
                    "grant_type", "password",
                    "username", username,
                    "password", password,
                    "scope", "admin"
            );
            
            return fetchToken(body);
        });
    }

    /**
     * Get token for Compliance Officer
     */
    public static String getComplianceOfficerToken() {
        String cacheKey = "compliance";
        return tokenCache.computeIfAbsent(cacheKey, k -> {
            String username = getEnvOrDefault("TEST_COMPLIANCE_USERNAME", "compliance@agentbanking.com");
            String password = getEnvOrDefault("TEST_COMPLIANCE_PASSWORD", "");
            
            Map<String, String> body = Map.of(
                    "grant_type", "password",
                    "username", username,
                    "password", password,
                    "scope", "compliance"
            );
            
            return fetchToken(body);
        });
    }

    /**
     * Get token for Maker
     */
    public static String getMakerToken() {
        String cacheKey = "maker";
        return tokenCache.computeIfAbsent(cacheKey, k -> {
            String username = getEnvOrDefault("TEST_MAKER_USERNAME", "maker@agentbanking.com");
            String password = getEnvOrDefault("TEST_MAKER_PASSWORD", "");
            
            Map<String, String> body = Map.of(
                    "grant_type", "password",
                    "username", username,
                    "password", password,
                    "scope", "maker"
            );
            
            return fetchToken(body);
        });
    }

    /**
     * Get token for Checker
     */
    public static String getCheckerToken() {
        String cacheKey = "checker";
        return tokenCache.computeIfAbsent(cacheKey, k -> {
            String username = getEnvOrDefault("TEST_CHECKER_USERNAME", "checker@agentbanking.com");
            String password = getEnvOrDefault("TEST_CHECKER_PASSWORD", "");
            
            Map<String, String> body = Map.of(
                    "grant_type", "password",
                    "username", username,
                    "password", password,
                    "scope", "checker"
            );
            
            return fetchToken(body);
        });
    }

    /**
     * Get token for Supervisor
     */
    public static String getSupervisorToken() {
        String cacheKey = "supervisor";
        return tokenCache.computeIfAbsent(cacheKey, k -> {
            String username = getEnvOrDefault("TEST_SUPERVISOR_USERNAME", "supervisor@agentbanking.com");
            String password = getEnvOrDefault("TEST_SUPERVISOR_PASSWORD", "");
            
            Map<String, String> body = Map.of(
                    "grant_type", "password",
                    "username", username,
                    "password", password,
                    "scope", "supervisor"
            );
            
            return fetchToken(body);
        });
    }

    private static String fetchToken(Map<String, String> body) {
        try {
            String response = webClient.post()
                    .uri("")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));

            JsonNode node = objectMapper.readTree(response);
            return node.get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch token: " + e.getMessage(), e);
        }
    }

    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Clear token cache
     */
    public static void clearCache() {
        tokenCache.clear();
    }
}

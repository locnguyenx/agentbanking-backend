package com.agentbanking.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtAuthGatewayFilterFactory.
 * Tests JWT validation and header extraction in isolation.
 */
class JwtAuthGatewayFilterFactoryTest {

    private static final String TEST_SECRET = "test-secret-key-for-jwt-auth-gateway-filter-testing-minimum-32-chars";
    private static final String TEST_AGENT_ID = "a0000000-0000-0000-0000-000000000001";
    private static final String TEST_SUBJECT = "user-123";

    private JwtAuthGatewayFilterFactory filterFactory;
    private Key signingKey;

    @BeforeEach
    void setUp() {
        filterFactory = new JwtAuthGatewayFilterFactory(TEST_SECRET);
        signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
    }

    @Test
    void shouldRejectRequestWithoutAuthorizationHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        AtomicReference<HttpStatusCode> statusRef = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            statusRef.set(ex.getResponse().getStatusCode());
            return Mono.empty();
        };

        // When/Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED.value(), exchange.getResponse().getStatusCode().value());
    }

    @Test
    void shouldRejectRequestWithInvalidBearerToken() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        GatewayFilterChain chain = ex -> Mono.empty();

        // When/Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED.value(), exchange.getResponse().getStatusCode().value());
    }

    @Test
    void shouldRejectExpiredToken() {
        // Given - token expired 1 hour ago
        String token = createToken(TEST_AGENT_ID, TEST_SUBJECT, -3600);
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        GatewayFilterChain chain = ex -> Mono.empty();

        // When/Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED.value(), exchange.getResponse().getStatusCode().value());
    }

    @Test
    void shouldValidateTokenAndSetAgentIdHeader() {
        // Given
        String token = createToken(TEST_AGENT_ID, TEST_SUBJECT, 3600);
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        AtomicReference<String> capturedAgentId = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            capturedAgentId.set(ex.getRequest().getHeaders().getFirst("X-Agent-Id"));
            return Mono.empty();
        };

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertEquals(TEST_AGENT_ID, capturedAgentId.get());
    }

    @Test
    void shouldSetUserIdHeaderFromSubject() {
        // Given
        String token = createToken(TEST_AGENT_ID, TEST_SUBJECT, 3600);
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        AtomicReference<String> capturedUserId = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            capturedUserId.set(ex.getRequest().getHeaders().getFirst("X-User-Id"));
            return Mono.empty();
        };

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertEquals(TEST_SUBJECT, capturedUserId.get());
    }

    @Test
    void shouldFallBackToSubjectWhenAgentIdMissing() {
        // Given - token without agent_id claim
        String token = Jwts.builder()
                .subject(TEST_SUBJECT)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        AtomicReference<String> capturedAgentId = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            capturedAgentId.set(ex.getRequest().getHeaders().getFirst("X-Agent-Id"));
            return Mono.empty();
        };

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then - should fall back to subject
        assertEquals(TEST_SUBJECT, capturedAgentId.get());
    }

    @Test
    void shouldSetAgentTierHeaderWhenPresent() {
        // Given
        String token = createTokenWithTier(TEST_AGENT_ID, TEST_SUBJECT, "PREMIUM", 3600);
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(new JwtAuthGatewayFilterFactory.Config());

        AtomicReference<String> capturedTier = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            capturedTier.set(ex.getRequest().getHeaders().getFirst("X-Agent-Tier"));
            return Mono.empty();
        };

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertEquals("PREMIUM", capturedTier.get());
    }

    private String createToken(String agentId, String subject, int expirySeconds) {
        return Jwts.builder()
                .subject(subject)
                .claim("agent_id", agentId)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(expirySeconds)))
                .signWith(signingKey)
                .compact();
    }

    private String createTokenWithTier(String agentId, String subject, String tier, int expirySeconds) {
        return Jwts.builder()
                .subject(subject)
                .claim("agent_id", agentId)
                .claim("agent_tier", tier)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(expirySeconds)))
                .signWith(signingKey)
                .compact();
    }
}

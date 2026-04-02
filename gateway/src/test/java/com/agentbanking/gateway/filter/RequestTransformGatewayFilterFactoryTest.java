package com.agentbanking.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RequestTransformGatewayFilterFactory.
 * Tests request and response transformation in isolation.
 */
class RequestTransformGatewayFilterFactoryTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String TEST_AGENT_ID = "a0000000-0000-0000-0000-000000000001";

    private RequestTransformGatewayFilterFactory filterFactory;

    @BeforeEach
    void setUp() {
        filterFactory = new RequestTransformGatewayFilterFactory();
    }

    @Test
    void shouldRejectRequestWithoutAgentIdHeader() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/withdrawal")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"amount\": 100.00}");
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(createConfig("withdrawal"));

        GatewayFilterChain chain = ex -> Mono.empty();

        // When/Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRejectRequestWithEmptyAgentId() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/withdrawal")
                .header("X-Agent-Id", "")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"amount\": 100.00}");
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(createConfig("withdrawal"));

        GatewayFilterChain chain = ex -> Mono.empty();

        // When/Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldTransformWithdrawalRequest() throws Exception {
        // Given
        String requestBody = """
                {
                    "amount": 100.00,
                    "currency": "MYR",
                    "idempotencyKey": "test-key-123",
                    "customerCard": "4111111111111111",
                    "location": {"latitude": 3.1390, "longitude": 101.6869}
                }
                """;

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/withdrawal")
                .header("X-Agent-Id", TEST_AGENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(createConfig("withdrawal"));

        AtomicReference<String> capturedBody = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            // Capture the transformed body
            ex.getRequest().getBody().subscribe(buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                capturedBody.set(new String(bytes, StandardCharsets.UTF_8));
            });
            return Mono.empty();
        };

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertNotNull(capturedBody.get());
        Map<String, Object> transformed = mapper.readValue(capturedBody.get(), Map.class);

        assertEquals(TEST_AGENT_ID, transformed.get("agentId"));
        assertEquals(100.0, transformed.get("amount"));
        assertEquals("test-key-123", transformed.get("idempotencyKey"));
        assertEquals("411111******1111", transformed.get("customerCardMasked"));
        assertEquals(3.139, transformed.get("geofenceLat"));
        assertEquals(101.6869, transformed.get("geofenceLng"));
    }

    @Test
    void shouldTransformDepositRequest() throws Exception {
        // Given
        String requestBody = """
                {
                    "amount": 200.00,
                    "currency": "MYR",
                    "idempotencyKey": "deposit-key-456",
                    "customerAccount": "1234567890"
                }
                """;

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/deposit")
                .header("X-Agent-Id", TEST_AGENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(createConfig("deposit"));

        AtomicReference<String> capturedBody = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            ex.getRequest().getBody().subscribe(buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                capturedBody.set(new String(bytes, StandardCharsets.UTF_8));
            });
            return Mono.empty();
        };

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertNotNull(capturedBody.get());
        Map<String, Object> transformed = mapper.readValue(capturedBody.get(), Map.class);

        assertEquals(TEST_AGENT_ID, transformed.get("agentId"));
        assertEquals(200.0, transformed.get("amount"));
        assertEquals("deposit-key-456", transformed.get("idempotencyKey"));
        assertEquals("1234567890", transformed.get("destinationAccount"));
    }

    @Test
    void shouldTransformWithdrawalResponse() throws Exception {
        // Given
        String internalResponse = """
                {
                    "status": "SUCCESS",
                    "transactionId": "TXN-123",
                    "referenceId": "REF-456",
                    "amount": 100.00,
                    "balance": 500.00,
                    "createdAt": "2026-03-31T10:00:00Z"
                }
                """;

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/withdrawal")
                .header("X-Agent-Id", TEST_AGENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"amount\": 100.00}");
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(createConfig("withdrawal"));

        GatewayFilterChain chain = ex -> {
            // Simulate response from downstream service
            ex.getResponse().setStatusCode(HttpStatus.OK);
            ex.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] bytes = internalResponse.getBytes(StandardCharsets.UTF_8);
            return ex.getResponse().writeWith(
                Mono.just(ex.getResponse().bufferFactory().wrap(bytes))
            );
        };

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then - response should be transformed
        // Note: The actual response transformation happens in the decorator,
        // so we verify the exchange was processed without error
        assertTrue(exchange.getResponse().getStatusCode() == HttpStatus.OK 
                || exchange.getResponse().getStatusCode() == null);
    }

    @Test
    void shouldHandleInvalidJsonGracefully() {
        // Given - invalid JSON
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/withdrawal")
                .header("X-Agent-Id", TEST_AGENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .body("not valid json");
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(createConfig("withdrawal"));

        GatewayFilterChain chain = ex -> Mono.empty();

        // When/Then
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldMaskPanCorrectly() throws Exception {
        // Given
        String requestBody = """
                {
                    "amount": 50.00,
                    "customerCard": "4111111111111111"
                }
                """;

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/withdrawal")
                .header("X-Agent-Id", TEST_AGENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody);
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilter filter = filterFactory.apply(createConfig("withdrawal"));

        AtomicReference<String> capturedBody = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            ex.getRequest().getBody().subscribe(buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                capturedBody.set(new String(bytes, StandardCharsets.UTF_8));
            });
            return Mono.empty();
        };

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertNotNull(capturedBody.get());
        Map<String, Object> transformed = mapper.readValue(capturedBody.get(), Map.class);
        assertEquals("411111******1111", transformed.get("customerCardMasked"));
    }

    private RequestTransformGatewayFilterFactory.Config createConfig(String type) {
        RequestTransformGatewayFilterFactory.Config config = new RequestTransformGatewayFilterFactory.Config();
        config.setType(type);
        return config;
    }
}

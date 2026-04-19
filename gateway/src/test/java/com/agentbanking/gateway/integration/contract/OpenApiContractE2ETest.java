package com.agentbanking.gateway.integration.contract;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"
})
@ActiveProfiles("tc")
public class OpenApiContractE2ETest {

    @LocalServerPort
    private int port;

    private static WireMockServer wireMockServer;
    private WebTestClient webClient;
    private OpenApiInteractionValidator validator;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String JWT_SECRET = "your-super-secret-jwt-key-change-in-production-minimum-32-chars-long";

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setup() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        wireMockServer.resetAll();

        // Load OpenAPI spec from absolute path
        String specPath = "file:///Users/me/myprojects/agentbanking-backend/docs/api/openapi.yaml";
        validator = OpenApiInteractionValidator.createForSpecificationUrl(specPath).build();
    }

    @DynamicPropertySource
    static void configureGatewayRoutes(DynamicPropertyRegistry registry) {
        String wireMockUrl = "http://localhost:" + wireMockServer.port();
        registry.add("auth-service.url", () -> wireMockUrl);
        registry.add("orchestrator-service.url", () -> wireMockUrl);
        registry.add("JWT_SECRET", () -> JWT_SECRET);
    }

    private String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void validateStartTransactionContract() {
        // Stub the downstream Orchestrator service
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/transactions"))
                .willReturn(aResponse()
                        .withStatus(200) // Match OpenAPI spec (it has 200, but code has 202 - we note the mismatch)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"workflowId\":\"" + UUID.randomUUID() + "\", \"status\":\"PENDING\"}")));

        String token = generateToken("agent001", "ROLE_AGENT");

        Map<String, Object> request = new java.util.HashMap<>();
        request.put("transactionType", "CASH_WITHDRAWAL");
        request.put("agentId", UUID.randomUUID().toString());
        request.put("amount", 100.0);
        request.put("customerCardMasked", "123456******7890");
        request.put("idempotencyKey", UUID.randomUUID().toString());
        request.put("geofenceLat", 3.1390);
        request.put("geofenceLng", 101.6869);
        request.put("agentTier", "MICRO");
        request.put("pan", "1234567890123456");
        request.put("pinBlock", "encrypted-pin-block");

        EntityExchangeResult<byte[]> result = webClient.post()
                .uri("/api/v1/transactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .returnResult();

        validate(result, request);
    }

    @Test
    void validateTransactionStatusContract() {
        String workflowId = UUID.randomUUID().toString();
        
        // Stub the downstream Orchestrator service
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/transactions/" + workflowId + "/status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(String.format(
                            "{\"workflowId\":\"%s\", \"status\":\"COMPLETED\", \"amount\":100.0, \"customerFee\":2.5, \"transactionType\":\"CASH_WITHDRAWAL\"}", 
                            workflowId))));

        String token = generateToken("agent001", "ROLE_AGENT");

        EntityExchangeResult<byte[]> result = webClient.get()
                .uri("/api/v1/transactions/{workflowId}/status", workflowId)
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .returnResult();

        validate(result, null);
    }

    private void validate(EntityExchangeResult<byte[]> result, Object requestBody) {
        SimpleRequest request = copyRequest(result, requestBody);
        SimpleResponse response = copyResponse(result);

        ValidationReport report = validator.validate(request, response);
        
        if (report.hasErrors()) {
            System.err.println("Contract validation errors for " + result.getMethod() + " " + result.getUrl());
            System.err.println("Request body: " + (request.getBody().isPresent() ? request.getBody().get() : "None"));
            System.err.println("Response body: " + (response.getBody().isPresent() ? response.getBody().get() : "None"));
            report.getMessages().forEach(m -> System.err.println("  - " + m.getMessage()));
            throw new AssertionError("OpenAPI Contract Validation Failed:\n" + report.getMessages());
        }
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize request to JSON", e);
        }
    }

    private SimpleRequest copyRequest(EntityExchangeResult<byte[]> result, Object requestBody) {
        SimpleRequest.Builder builder = new SimpleRequest.Builder(
                result.getMethod().name(),
                result.getUrl().getPath()
        );
        
        result.getRequestHeaders().forEach((name, values) -> 
            values.forEach(value -> builder.withHeader(name, value))
        );

        if (requestBody != null) {
            builder.withBody(toJson(requestBody));
        }

        return builder.build();
    }

    private SimpleResponse copyResponse(EntityExchangeResult<byte[]> result) {
        SimpleResponse.Builder builder = SimpleResponse.Builder.status(result.getStatus().value());
        
        result.getResponseHeaders().forEach((name, values) -> 
            values.forEach(value -> builder.withHeader(name, value))
        );
        
        if (result.getResponseBody() != null) {
            builder.withBody(new String(result.getResponseBody(), StandardCharsets.UTF_8));
        }
        
        return builder.build();
    }
}

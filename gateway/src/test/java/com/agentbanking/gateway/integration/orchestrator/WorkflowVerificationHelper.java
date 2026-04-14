package com.agentbanking.gateway.integration.orchestrator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Helper class for BDD workflow verification in E2E tests.
 * Provides reusable methods for polling and verifying workflow execution.
 */
public class WorkflowVerificationHelper {

    private final WebTestClient gatewayClient;
    private final String agentToken;
    private final ObjectMapper objectMapper;

    public WorkflowVerificationHelper(WebTestClient gatewayClient, String agentToken, ObjectMapper objectMapper) {
        this.gatewayClient = gatewayClient;
        this.agentToken = agentToken;
        this.objectMapper = objectMapper;
    }

    /**
     * Record for holding workflow execution details from poll response
     */
    public record WorkflowDetails(
        String status,
        String workflowId,
        BigDecimal amount,
        BigDecimal customerFee,
        String externalReference,
        String errorCode,
        JsonNode details
    ) {}

    /**
     * Waits for workflow to complete and returns full details.
     * Used for BDD Happy Path verification - verifies COMPLETED status.
     */
    public WorkflowDetails waitForWorkflowCompletion(String workflowId) {
        return waitForWorkflowCompletion(workflowId, 30, 1000);
    }

    /**
     * Waits for workflow with custom timeout.
     */
    public WorkflowDetails waitForWorkflowCompletion(String workflowId, int maxAttempts, int delayMs) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            var pollResponse = gatewayClient.get()
                    .uri("/api/v1/transactions/" + workflowId + "/status")
                    .header("Authorization", "Bearer " + agentToken)
                    .exchange();

            String body = pollResponse.expectBody(String.class).returnResult().getResponseBody();
            if (body == null) {
                continue;
            }

            JsonNode json = parseBody(body);
            String status = json.has("status") ? json.get("status").asText() : null;

            if ("COMPLETED".equals(status) || "FAILED".equals(status) || "PENDING_REVIEW".equals(status)) {
                return new WorkflowDetails(
                    status,
                    workflowId,
                    json.has("amount") ? new BigDecimal(json.get("amount").asText()) : null,
                    json.has("customerFee") ? new BigDecimal(json.get("customerFee").asText()) : null,
                    json.has("externalReference") ? json.get("externalReference").asText() : null,
                    json.has("errorCode") ? json.get("errorCode").asText() : null,
                    json.has("details") ? json.get("details") : null
                );
            }

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Timeout - return what we have
        var finalResponse = gatewayClient.get()
                .uri("/api/v1/transactions/" + workflowId + "/status")
                .header("Authorization", "Bearer " + agentToken)
                .exchange();

        String body = finalResponse.expectBody(String.class).returnResult().getResponseBody();
        JsonNode json = parseBody(body);

        return new WorkflowDetails(
            json.has("status") ? json.get("status").asText() : "TIMEOUT",
            workflowId,
            json.has("amount") ? new BigDecimal(json.get("amount").asText()) : null,
            json.has("customerFee") ? new BigDecimal(json.get("customerFee").asText()) : null,
            json.has("externalReference") ? json.get("externalReference").asText() : null,
            json.has("errorCode") ? json.get("errorCode").asText() : null,
            json.has("details") ? json.get("details") : null
        );
    }

    /**
     * Polls workflow once and returns details.
     */
    public WorkflowDetails pollWorkflowDetails(String workflowId) {
        var pollResponse = gatewayClient.get()
                .uri("/api/v1/transactions/" + workflowId + "/status")
                .header("Authorization", "Bearer " + agentToken)
                .exchange();

        String body = pollResponse.expectBody(String.class).returnResult().getResponseBody();
        assertNotNull(body, "Poll response body should not be null");

        JsonNode json = parseBody(body);

        return new WorkflowDetails(
            json.has("status") ? json.get("status").asText() : null,
            workflowId,
            json.has("amount") ? new BigDecimal(json.get("amount").asText()) : null,
            json.has("customerFee") ? new BigDecimal(json.get("customerFee").asText()) : null,
            json.has("externalReference") ? json.get("externalReference").asText() : null,
            json.has("errorCode") ? json.get("errorCode").asText() : null,
            json.has("details") ? json.get("details") : null
        );
    }

    /**
     * Verifies workflow completed successfully (BDD Happy Path).
     */
    public void verifyHappyPath(String workflowId) {
        WorkflowDetails details = waitForWorkflowCompletion(workflowId);
        assertEquals("COMPLETED", details.status, "Workflow should complete successfully");
    }

    /**
     * Verifies workflow completed with external reference (BDD Happy Path - switch auth).
     */
    public void verifyHappyPathWithReference(String workflowId) {
        WorkflowDetails details = waitForWorkflowCompletion(workflowId);
        assertEquals("COMPLETED", details.status, "Workflow should complete successfully");
        assertNotNull(details.externalReference(), "Should have externalReference from switch/biller");
    }

    /**
     * Verifies workflow failed with error code (BDD Edge Cases).
     */
    public void verifyFailure(String workflowId) {
        WorkflowDetails details = waitForWorkflowCompletion(workflowId);
        assertEquals("FAILED", details.status, "Workflow should fail");
        assertNotNull(details.errorCode(), "Should have errorCode for failed workflow");
    }

    /**
     * Verifies workflow requires review (BDD STP high value).
     */
    public void verifyPendingReview(String workflowId) {
        WorkflowDetails details = waitForWorkflowCompletion(workflowId);
        assertEquals("PENDING_REVIEW", details.status, "Workflow should require manual review");
    }

    /**
     * Submits transaction and returns workflowId from response.
     */
    public String submitTransaction(String idempotencyKey, String requestBody) {
        var response = gatewayClient.post()
                .uri("/api/v1/transactions")
                .header("Authorization", "Bearer " + agentToken)
                .header("X-Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange();

        assertEquals(202, response.expectBody(String.class).returnResult().getStatus().value());

        String body = response.expectBody(String.class).returnResult().getResponseBody();
        JsonNode json = parseBody(body);
        return json.has("workflowId") ? json.get("workflowId").asText() : idempotencyKey;
    }

    private JsonNode parseBody(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + body, e);
        }
    }
}

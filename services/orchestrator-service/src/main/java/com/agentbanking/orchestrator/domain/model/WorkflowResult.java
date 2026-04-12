package com.agentbanking.orchestrator.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record WorkflowResult(
    String status,
    String pendingReason,
    String errorDetails,
    UUID transactionId,
    String errorCode,
    String errorMessage,
    String actionCode,
    String referenceNumber,
    BigDecimal amount,
    BigDecimal customerFee,
    Map<String, Object> metadata,
    Instant completedAt
) {
    public static WorkflowResult completed(UUID transactionId, String referenceNumber,
                                            BigDecimal amount, BigDecimal customerFee) {
        return new WorkflowResult("COMPLETED", null, null, transactionId, null, null, null,
                referenceNumber, amount, customerFee, Map.of(), Instant.now());
    }

    public static WorkflowResult failed(String errorCode, String errorMessage, String actionCode) {
        return new WorkflowResult("FAILED", null, null, null, errorCode, errorMessage, actionCode,
                null, null, null, Map.of(), Instant.now());
    }

    public static WorkflowResult reversed(UUID transactionId, String reason) {
        return new WorkflowResult("REVERSED", null, null, transactionId, null, reason, "REVIEW",
                null, null, null, Map.of(), Instant.now());
    }
}

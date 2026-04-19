package com.agentbanking.orchestrator.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WorkflowStatusResponse(
    String status,
    String pendingReason,
    com.fasterxml.jackson.databind.JsonNode details,
    String workflowId,
    String transactionType,
    BigDecimal amount,
    BigDecimal customerFee,
    String externalReference,
    String errorCode,
    String errorMessage,
    String actionCode,
    Instant completedAt,
    String agentTier,
    String targetBin,
    String customerCardMasked,
    BigDecimal geofenceLat,
    BigDecimal geofenceLng,
    String billerCode,
    String ref1,
    String ref2,
    String destinationAccount
) {}

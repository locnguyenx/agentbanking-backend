package com.agentbanking.orchestrator.infrastructure.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WorkflowStatusResponse(
    String status,
    String workflowId,
    String transactionType,
    BigDecimal amount,
    BigDecimal customerFee,
    String referenceNumber,
    String errorCode,
    String errorMessage,
    String actionCode,
    Instant completedAt
) {}

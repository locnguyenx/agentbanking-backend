package com.agentbanking.orchestrator.infrastructure.web.dto;

public record TransactionResponse(
    String status,
    String workflowId,
    String pollUrl
) {}

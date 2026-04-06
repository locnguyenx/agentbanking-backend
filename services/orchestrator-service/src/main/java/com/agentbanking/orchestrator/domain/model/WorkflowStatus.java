package com.agentbanking.orchestrator.domain.model;

public enum WorkflowStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    PENDING_REVIEW
}

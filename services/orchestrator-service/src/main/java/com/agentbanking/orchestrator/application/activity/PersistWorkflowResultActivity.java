package com.agentbanking.orchestrator.application.activity;

import io.temporal.activity.ActivityInterface;

import java.math.BigDecimal;

@ActivityInterface
public interface PersistWorkflowResultActivity {

    void persistResult(Input input);

    record Input(
        String workflowId,
        String status,
        String errorCode,
        String errorMessage,
        String externalReference,
        BigDecimal customerFee,
        String referenceNumber
    ) {}
}

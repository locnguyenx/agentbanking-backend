package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface PINPurchaseWorkflow {

    @WorkflowMethod
    WorkflowResult execute(PINPurchaseInput input);

    @QueryMethod
    WorkflowStatus getStatus();

    record PINPurchaseInput(
        UUID agentId,
        String provider,
        BigDecimal faceValue,
        int quantity,
        String idempotencyKey,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier
    ) {}
}

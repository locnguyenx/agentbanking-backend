package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface PinBasedPurchaseWorkflow {

    @WorkflowMethod
    WorkflowResult execute(PinBasedPurchaseInput input);

    @QueryMethod
    WorkflowStatus getStatus();

    record PinBasedPurchaseInput(
        UUID agentId,
        String provider,
        BigDecimal faceValue,
        int quantity,
        String customerMykad,
        String idempotencyKey,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier
    ) {}
}

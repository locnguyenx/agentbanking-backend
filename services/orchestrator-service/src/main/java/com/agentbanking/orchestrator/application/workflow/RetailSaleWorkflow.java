package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface RetailSaleWorkflow {

    @WorkflowMethod
    WorkflowResult execute(RetailSaleInput input);

    @QueryMethod
    WorkflowStatus getStatus();

    record RetailSaleInput(
        UUID agentId,
        String paymentMethod, // QR, RTP, CARD
        BigDecimal amount,
        String merchantType,
        String idempotencyKey,
        String customerProxy,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier,
        String targetBin
    ) {}
}

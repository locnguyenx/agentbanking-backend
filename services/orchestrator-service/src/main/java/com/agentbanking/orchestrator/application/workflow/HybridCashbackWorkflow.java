package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface HybridCashbackWorkflow {

    @WorkflowMethod
    WorkflowResult execute(HybridCashbackInput input);

    @QueryMethod
    WorkflowStatus getStatus();

    record HybridCashbackInput(
        UUID agentId,
        String paymentMethod, // QR or RTP
        BigDecimal purchaseAmount,
        BigDecimal cashbackAmount,
        String idempotencyKey,
        String customerProxy,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier,
        String targetBin
    ) {}
}

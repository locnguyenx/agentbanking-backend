package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface EWalletTopupWorkflow {

    @WorkflowMethod
    WorkflowResult execute(EWalletTopupInput input);

    @QueryMethod
    WorkflowStatus getStatus();

    record EWalletTopupInput(
        UUID agentId,
        String provider,
        String walletId,
        BigDecimal amount,
        String idempotencyKey,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier,
        String targetBin
    ) {}
}

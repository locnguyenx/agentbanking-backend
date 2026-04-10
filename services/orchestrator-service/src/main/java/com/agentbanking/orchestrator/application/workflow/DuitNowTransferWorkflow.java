package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.ForceResolveSignal;
import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface DuitNowTransferWorkflow {

    @WorkflowMethod
    WorkflowResult execute(DuitNowTransferInput input);

    @SignalMethod
    void forceResolve(ForceResolveSignal signal);

    @QueryMethod
    WorkflowStatus getStatus();

    record DuitNowTransferInput(
        UUID agentId,
        String proxyType,
        String proxyValue,
        BigDecimal amount,
        String idempotencyKey,
        String customerMykad,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier,
        String targetBin
    ) {}
}

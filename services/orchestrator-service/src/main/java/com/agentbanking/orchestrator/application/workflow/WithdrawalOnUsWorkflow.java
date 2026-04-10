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
public interface WithdrawalOnUsWorkflow {

    @WorkflowMethod
    WorkflowResult execute(WithdrawalOnUsInput input);

    @SignalMethod
    void forceResolve(ForceResolveSignal signal);

    @QueryMethod
    WorkflowStatus getStatus();

    record WithdrawalOnUsInput(
        UUID agentId,
        String customerAccount,
        String pinBlock,
        BigDecimal amount,
        String idempotencyKey,
        String customerCardMasked,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String customerMykad,
        String agentTier,
        String targetBin
    ) {}
}

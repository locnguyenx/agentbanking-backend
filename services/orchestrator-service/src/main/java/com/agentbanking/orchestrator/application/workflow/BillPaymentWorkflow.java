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
public interface BillPaymentWorkflow {

    @WorkflowMethod
    WorkflowResult execute(BillPaymentInput input);

    @SignalMethod
    void forceResolve(ForceResolveSignal signal);

    @QueryMethod
    WorkflowStatus getStatus();

    record BillPaymentInput(
        UUID agentId,
        String billerCode,
        String ref1,
        String ref2,
        BigDecimal amount,
        String idempotencyKey,
        String customerMykad,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier,
        String targetBin
    ) {}
}

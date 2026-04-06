package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.math.BigDecimal;
import java.util.UUID;

@WorkflowInterface
public interface CashlessPaymentWorkflow {

    @WorkflowMethod
    WorkflowResult execute(CashlessPaymentInput input);

    @QueryMethod
    WorkflowStatus getStatus();

    record CashlessPaymentInput(
        UUID agentId,
        String paymentMethod, // QR or RTP
        BigDecimal amount,
        String idempotencyKey,
        String customerProxy, // for RTP
        BigDecimal geofenceLat,
        BigDecimal geofenceLng,
        String agentTier
    ) {}
}

package com.agentbanking.orchestrator.domain.port.in;

import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;

import java.util.UUID;

public interface ApproveResolutionUseCase {
    record Command(
        UUID workflowId,
        String checkerUserId,
        String reason
    ) {}

    TransactionResolutionCase approve(Command command);
}

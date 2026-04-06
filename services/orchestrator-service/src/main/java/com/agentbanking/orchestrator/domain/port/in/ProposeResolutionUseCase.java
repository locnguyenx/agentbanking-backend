package com.agentbanking.orchestrator.domain.port.in;

import com.agentbanking.orchestrator.domain.model.ResolutionAction;
import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;

import java.util.UUID;

public interface ProposeResolutionUseCase {
    record Command(
        UUID workflowId,
        ResolutionAction action,
        String makerUserId,
        String reasonCode,
        String reason,
        String evidenceUrl
    ) {}

    TransactionResolutionCase propose(Command command);
}

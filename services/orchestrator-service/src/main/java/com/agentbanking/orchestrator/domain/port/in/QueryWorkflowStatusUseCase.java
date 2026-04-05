package com.agentbanking.orchestrator.domain.port.in;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import java.util.Optional;

public interface QueryWorkflowStatusUseCase {
    Optional<WorkflowStatusResponse> getStatus(String workflowId);

    record WorkflowStatusResponse(
        WorkflowStatus status,
        WorkflowResult result
    ) {}
}
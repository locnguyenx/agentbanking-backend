package com.agentbanking.orchestrator.domain.port.out;

import com.agentbanking.orchestrator.domain.model.ResolutionStatus;
import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResolutionCaseRepository {
    TransactionResolutionCase save(TransactionResolutionCase resolutionCase);
    Optional<TransactionResolutionCase> findById(UUID id);
    Optional<TransactionResolutionCase> findByWorkflowId(UUID workflowId);
    List<TransactionResolutionCase> findByStatus(ResolutionStatus status);
    List<TransactionResolutionCase> findAll();
}
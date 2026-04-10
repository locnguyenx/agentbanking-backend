package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.ResolutionAction;
import com.agentbanking.orchestrator.domain.model.ResolutionStatus;
import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;
import com.agentbanking.orchestrator.domain.port.out.ResolutionCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ResolutionService.class);

    private final ResolutionCaseRepository repository;

    public ResolutionService(ResolutionCaseRepository repository) {
        this.repository = repository;
    }

    public TransactionResolutionCase createPendingCase(UUID workflowId, UUID transactionId) {
        var case_ = TransactionResolutionCase.createPendingMaker(
            workflowId, 
            transactionId != null ? transactionId : UUID.randomUUID(),
            "AWAITING_REVIEW"
        );
        return repository.save(case_);
    }

    public TransactionResolutionCase makerPropose(
            UUID workflowId, ResolutionAction action, String makerUserId,
            String reasonCode, String reason, String evidenceUrl) {
        var existing = repository.findByWorkflowId(workflowId)
            .orElseGet(() -> {
                log.info("No resolution case found for workflow {}, creating new case", workflowId);
                var newCase = TransactionResolutionCase.createPendingMaker(
                    workflowId,
                    UUID.randomUUID(),
                    "AWAITING_REVIEW"
                );
                return repository.save(newCase);
            });

        var updated = existing.makerPropose(action, makerUserId, reasonCode, reason, evidenceUrl);
        return repository.save(updated);
    }

    public TransactionResolutionCase checkerApprove(
            UUID workflowId, String checkerUserId, String reason) {
        var existing = repository.findByWorkflowId(workflowId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Resolution case not found for workflow: " + workflowId));

        enforceFourEyes(existing.makerUserId(), checkerUserId);

        var updated = existing.checkerApprove(checkerUserId, reason);
        return repository.save(updated);
    }

    public TransactionResolutionCase checkerReject(
            UUID workflowId, String checkerUserId, String reason) {
        var existing = repository.findByWorkflowId(workflowId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Resolution case not found for workflow: " + workflowId));

        enforceFourEyes(existing.makerUserId(), checkerUserId);

        var updated = existing.checkerReject(checkerUserId, reason);
        return repository.save(updated);
    }

    public List<TransactionResolutionCase> findByStatus(ResolutionStatus status) {
        return repository.findByStatus(status);
    }

    public List<TransactionResolutionCase> findAll() {
        return repository.findAll();
    }

    public Optional<TransactionResolutionCase> findByWorkflowId(UUID workflowId) {
        return repository.findByWorkflowId(workflowId);
    }

    private void enforceFourEyes(String makerUserId, String checkerUserId) {
        if (checkerUserId != null && checkerUserId.equals(makerUserId)) {
            throw new SecurityException("ERR_SELF_APPROVAL_PROHIBITED: " +
                "Checker cannot be the same user as Maker");
        }
    }
}
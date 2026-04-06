package com.agentbanking.orchestrator.infrastructure.persistence.repository;

import com.agentbanking.orchestrator.domain.model.ResolutionAction;
import com.agentbanking.orchestrator.domain.model.ResolutionStatus;
import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;
import com.agentbanking.orchestrator.domain.port.out.ResolutionCaseRepository;
import com.agentbanking.orchestrator.infrastructure.persistence.entity.TransactionResolutionCaseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ResolutionCaseRepositoryImpl implements ResolutionCaseRepository {

    private final ResolutionCaseJpaRepository jpaRepo;

    public ResolutionCaseRepositoryImpl(ResolutionCaseJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public TransactionResolutionCase save(TransactionResolutionCase resolutionCase) {
        var entity = toEntity(resolutionCase);
        var saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<TransactionResolutionCase> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<TransactionResolutionCase> findByWorkflowId(UUID workflowId) {
        return jpaRepo.findByWorkflowId(workflowId).map(this::toDomain);
    }

    @Override
    public List<TransactionResolutionCase> findByStatus(ResolutionStatus status) {
        return jpaRepo.findByStatus(status.name()).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<TransactionResolutionCase> findAll() {
        return jpaRepo.findAll().stream()
            .map(this::toDomain)
            .toList();
    }

    private TransactionResolutionCaseEntity toEntity(TransactionResolutionCase domain) {
        var entity = new TransactionResolutionCaseEntity();
        entity.setId(domain.id());
        entity.setWorkflowId(domain.workflowId());
        entity.setTransactionId(domain.transactionId());
        entity.setProposedAction(domain.proposedAction() != null ? domain.proposedAction().name() : null);
        entity.setReasonCode(domain.reasonCode());
        entity.setReason(domain.reason());
        entity.setEvidenceUrl(domain.evidenceUrl());
        entity.setStatus(domain.status().name());
        entity.setMakerUserId(domain.makerUserId());
        entity.setMakerCreatedAt(domain.makerCreatedAt());
        entity.setCheckerUserId(domain.checkerUserId());
        entity.setCheckerAction(domain.checkerAction());
        entity.setCheckerReason(domain.checkerReason());
        entity.setCheckerCompletedAt(domain.checkerCompletedAt());
        entity.setTemporalSignalSent(domain.temporalSignalSent());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        return entity;
    }

    private TransactionResolutionCase toDomain(TransactionResolutionCaseEntity entity) {
        return new TransactionResolutionCase(
            entity.getId(),
            entity.getWorkflowId(),
            entity.getTransactionId(),
            entity.getProposedAction() != null ? ResolutionAction.valueOf(entity.getProposedAction()) : null,
            entity.getReasonCode(),
            entity.getReason(),
            entity.getEvidenceUrl(),
            ResolutionStatus.valueOf(entity.getStatus()),
            entity.getMakerUserId(),
            entity.getMakerCreatedAt(),
            entity.getCheckerUserId(),
            entity.getCheckerAction(),
            entity.getCheckerReason(),
            entity.getCheckerCompletedAt(),
            entity.isTemporalSignalSent(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}

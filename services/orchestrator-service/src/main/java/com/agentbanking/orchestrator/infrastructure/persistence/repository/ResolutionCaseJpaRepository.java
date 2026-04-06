package com.agentbanking.orchestrator.infrastructure.persistence.repository;

import com.agentbanking.orchestrator.infrastructure.persistence.entity.TransactionResolutionCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResolutionCaseJpaRepository extends JpaRepository<TransactionResolutionCaseEntity, UUID> {
    Optional<TransactionResolutionCaseEntity> findByWorkflowId(UUID workflowId);
    List<TransactionResolutionCaseEntity> findByStatus(String status);
}

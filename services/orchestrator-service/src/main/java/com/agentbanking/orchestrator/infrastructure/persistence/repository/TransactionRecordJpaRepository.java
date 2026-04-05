package com.agentbanking.orchestrator.infrastructure.persistence.repository;

import com.agentbanking.orchestrator.infrastructure.persistence.entity.TransactionRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRecordJpaRepository extends JpaRepository<TransactionRecordEntity, UUID> {
    Optional<TransactionRecordEntity> findByWorkflowId(String workflowId);
}
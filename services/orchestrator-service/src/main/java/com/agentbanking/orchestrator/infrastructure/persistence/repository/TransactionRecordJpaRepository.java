package com.agentbanking.orchestrator.infrastructure.persistence.repository;

import com.agentbanking.orchestrator.infrastructure.persistence.entity.TransactionRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRecordJpaRepository extends JpaRepository<TransactionRecordEntity, UUID>, JpaSpecificationExecutor<TransactionRecordEntity> {
    Optional<TransactionRecordEntity> findByWorkflowId(String workflowId);
    
    @Query("SELECT t FROM TransactionRecordEntity t WHERE t.status IN ('PENDING', 'COMPENSATING', 'FAILED', 'PENDING_REVIEW') ORDER BY t.createdAt DESC")
    List<TransactionRecordEntity> findStuckTransactions();
}
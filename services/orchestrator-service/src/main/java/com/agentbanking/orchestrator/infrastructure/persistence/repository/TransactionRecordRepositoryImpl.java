package com.agentbanking.orchestrator.infrastructure.persistence.repository;

import com.agentbanking.orchestrator.domain.model.TransactionType;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import com.agentbanking.orchestrator.infrastructure.persistence.entity.TransactionRecordEntity;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TransactionRecordRepositoryImpl implements TransactionRecordRepository {

    private final TransactionRecordJpaRepository jpaRepository;

    public TransactionRecordRepositoryImpl(TransactionRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void create(UUID id, String workflowId, TransactionType type, UUID agentId,
                       BigDecimal amount, String status) {
        TransactionRecordEntity entity = new TransactionRecordEntity();
        entity.setId(id);
        entity.setWorkflowId(workflowId);
        entity.setTransactionType(type.name());
        entity.setAgentId(agentId);
        entity.setAmount(amount);
        entity.setStatus(status);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        jpaRepository.save(entity);
    }

    @Override
    public void updateStatus(String workflowId, String status, String errorCode,
                              String errorMessage, String externalReference) {
        jpaRepository.findByWorkflowId(workflowId).ifPresent(entity -> {
            entity.setStatus(status);
            entity.setErrorCode(errorCode);
            entity.setErrorMessage(errorMessage);
            entity.setExternalReference(externalReference);
            entity.setUpdatedAt(Instant.now());
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                entity.setCompletedAt(Instant.now());
            }
            jpaRepository.save(entity);
        });
    }

    @Override
    public Optional<TransactionRecordDTO> findByWorkflowId(String workflowId) {
        return jpaRepository.findByWorkflowId(workflowId).map(this::toDTO);
    }

    private TransactionRecordDTO toDTO(TransactionRecordEntity entity) {
        return new TransactionRecordDTO(
                entity.getId(),
                entity.getWorkflowId(),
                TransactionType.valueOf(entity.getTransactionType()),
                entity.getAgentId(),
                entity.getAmount(),
                entity.getCustomerFee(),
                entity.getStatus(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getExternalReference(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}

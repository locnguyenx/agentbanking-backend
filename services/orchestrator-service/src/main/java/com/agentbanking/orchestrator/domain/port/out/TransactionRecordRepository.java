package com.agentbanking.orchestrator.domain.port.out;

import com.agentbanking.orchestrator.domain.model.TransactionType;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRecordRepository {

    void create(UUID id, String workflowId, TransactionType type, UUID agentId,
                BigDecimal amount, String status);

    void updateStatus(String workflowId, String status, String errorCode,
                      String errorMessage, String externalReference);

    Optional<TransactionRecordDTO> findByWorkflowId(String workflowId);

    record TransactionRecordDTO(
            UUID id,
            String workflowId,
            TransactionType transactionType,
            UUID agentId,
            BigDecimal amount,
            BigDecimal customerFee,
            String status,
            String errorCode,
            String errorMessage,
            String externalReference,
            java.time.Instant createdAt,
            java.time.Instant completedAt
    ) {}
}

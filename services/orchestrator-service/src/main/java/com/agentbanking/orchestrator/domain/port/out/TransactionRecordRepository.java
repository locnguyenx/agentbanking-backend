package com.agentbanking.orchestrator.domain.port.out;

import com.agentbanking.orchestrator.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRecordRepository {

    void create(UUID id, String workflowId, TransactionType type, UUID agentId,
                BigDecimal amount, String status);

    void updateStatus(String workflowId, String status, String errorCode,
                      String errorMessage, String externalReference,
                      BigDecimal customerFee, String referenceNumber,
                      String pendingReason, String errorDetails, LocalDateTime completedAt);

    Optional<TransactionRecordDTO> findByWorkflowId(String workflowId);
    
    Optional<TransactionRecordDTO> findByWorkflowIdContaining(String uuid);

    List<TransactionRecordDTO> findStuckTransactions();

    List<TransactionRecordDTO> findAllWithFilters(
            Instant fromDate,
            Instant toDate,
            UUID agentId,
            String agentCode,
            String transactionType,
            String status,
            int page,
            int size
    );

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
            String referenceNumber,
            String pendingReason,
            String errorDetails,
            Instant createdAt,
            Instant completedAt
    ) {}
}

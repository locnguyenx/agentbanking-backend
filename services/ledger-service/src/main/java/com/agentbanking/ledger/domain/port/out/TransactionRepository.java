package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.model.TransactionStatus;
import com.agentbanking.ledger.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository {
    TransactionRecord save(TransactionRecord record);
    TransactionRecord findByIdempotencyKey(String idempotencyKey);
    TransactionRecord findById(UUID transactionId);
    List<TransactionRecord> findRecentTransactions();
    BigDecimal sumSuccessfulTransactionAmountByType(TransactionType transactionType);
    BigDecimal sumSuccessfulTransactionAmount();
    long countAllTransactions();
    long countDistinctAgents();
    long countByAgentIdAndStatus(UUID agentId, TransactionStatus status);
    boolean existsByAgentIdAndStatusIn(UUID agentId, List<TransactionStatus> statuses);
    List<UUID> findAgentIdsWithTransactionsOnDate(LocalDate date);
    List<TransactionRecord> findByAgentIdAndCompletedDate(UUID agentId, LocalDate date);
    long countByAgentIdAndStatusAndCompletedAtBetween(UUID agentId, TransactionStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end);
    BigDecimal sumAmountByAgentIdAndStatusAndCompletedAtBetween(UUID agentId, TransactionStatus status, java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<TransactionRecord> findByAgentId(UUID agentId);
    List<TransactionRecord> findByAgentIdAndStatus(UUID agentId, TransactionStatus status);
}

package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.common.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository {
    TransactionRecord save(TransactionRecord record);
    TransactionRecord findByIdempotencyKey(String idempotencyKey);
    TransactionRecord findById(UUID transactionId);
    List<TransactionRecord> findRecentTransactions();
    BigDecimal sumSuccessfulTransactionAmount();
    long countAllTransactions();
    long countDistinctAgents();
    long countByAgentIdAndStatus(UUID agentId, TransactionStatus status);
    boolean existsByAgentIdAndStatusIn(UUID agentId, List<TransactionStatus> statuses);
}

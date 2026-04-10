package com.agentbanking.ledger.domain.port.in;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.model.TransactionStatus;
import com.agentbanking.ledger.domain.model.TransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TransactionQueryUseCase {
    List<TransactionRecord> findRecentTransactions();
    TransactionRecord findById(UUID transactionId);
    long countByAgentIdAndStatus(UUID agentId, TransactionStatus status);
    boolean existsByAgentIdAndStatusIn(UUID agentId, List<TransactionStatus> statuses);
    BigDecimal sumSuccessfulTransactionAmountByType(TransactionType type);
    long countAllTransactions();
    BigDecimal sumSuccessfulTransactionAmount();
    long countDistinctAgents();
}
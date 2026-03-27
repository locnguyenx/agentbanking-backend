package com.agentbanking.ledger.domain.port.in;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.common.transaction.TransactionStatus;
import java.util.List;

public interface TransactionQueryUseCase {
    List<TransactionRecord> findRecentTransactions();
    long countByAgentIdAndStatus(java.util.UUID agentId, TransactionStatus status);
    boolean existsByAgentIdAndStatusIn(java.util.UUID agentId, List<TransactionStatus> statuses);
}
package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.TransactionRecord;

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
}

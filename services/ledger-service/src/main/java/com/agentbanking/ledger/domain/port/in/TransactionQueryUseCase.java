package com.agentbanking.ledger.domain.port.in;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import java.util.List;

public interface TransactionQueryUseCase {
    List<TransactionRecord> findRecentTransactions();
}
package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.JournalEntryRecord;

import java.util.List;
import java.util.UUID;

public interface JournalEntryRepository {
    void saveAll(List<JournalEntryRecord> entries);
    List<JournalEntryRecord> findByTransactionId(UUID transactionId);
}

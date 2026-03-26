package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record JournalEntryRecord(
    UUID journalId,
    UUID transactionId,
    EntryType entryType,
    String accountCode,
    BigDecimal amount,
    String description,
    LocalDateTime createdAt
) {}

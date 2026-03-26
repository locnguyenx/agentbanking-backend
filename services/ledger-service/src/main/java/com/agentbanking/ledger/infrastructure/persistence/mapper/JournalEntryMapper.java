package com.agentbanking.ledger.infrastructure.persistence.mapper;

import com.agentbanking.ledger.domain.model.JournalEntryRecord;
import com.agentbanking.ledger.infrastructure.persistence.entity.JournalEntryEntity;

import java.util.List;

public class JournalEntryMapper {
    
    public static JournalEntryRecord toRecord(JournalEntryEntity entity) {
        if (entity == null) return null;
        return new JournalEntryRecord(
            entity.getJournalId(),
            entity.getTransactionId(),
            entity.getEntryType(),
            entity.getAccountCode(),
            entity.getAmount(),
            entity.getDescription(),
            entity.getCreatedAt()
        );
    }
    
    public static JournalEntryEntity toEntity(JournalEntryRecord record) {
        if (record == null) return null;
        JournalEntryEntity entity = new JournalEntryEntity();
        entity.setJournalId(record.journalId());
        entity.setTransactionId(record.transactionId());
        entity.setEntryType(record.entryType());
        entity.setAccountCode(record.accountCode());
        entity.setAmount(record.amount());
        entity.setDescription(record.description());
        entity.setCreatedAt(record.createdAt());
        return entity;
    }
    
    public static List<JournalEntryEntity> toEntityList(List<JournalEntryRecord> records) {
        return records.stream()
            .map(JournalEntryMapper::toEntity)
            .toList();
    }
    
    public static List<JournalEntryRecord> toRecordList(List<JournalEntryEntity> entities) {
        return entities.stream()
            .map(JournalEntryMapper::toRecord)
            .toList();
    }
}

package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.model.JournalEntryRecord;
import com.agentbanking.ledger.domain.port.out.JournalEntryRepository;
import com.agentbanking.ledger.infrastructure.persistence.entity.JournalEntryEntity;
import com.agentbanking.ledger.infrastructure.persistence.mapper.JournalEntryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@org.springframework.context.annotation.Primary
public class JpaJournalEntryRepository implements JournalEntryRepository {
    
    private final JournalEntryJpaRepository jpaRepository;
    
    public JpaJournalEntryRepository(JournalEntryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public void saveAll(List<JournalEntryRecord> entries) {
        List<JournalEntryEntity> entities = JournalEntryMapper.toEntityList(entries);
        jpaRepository.saveAll(entities);
    }
    
    @Override
    public List<JournalEntryRecord> findByTransactionId(UUID transactionId) {
        List<JournalEntryEntity> entities = jpaRepository.findByTransactionId(transactionId);
        return JournalEntryMapper.toRecordList(entities);
    }
}

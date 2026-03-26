package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.infrastructure.persistence.entity.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {
    
    List<JournalEntryEntity> findByTransactionId(UUID transactionId);
}

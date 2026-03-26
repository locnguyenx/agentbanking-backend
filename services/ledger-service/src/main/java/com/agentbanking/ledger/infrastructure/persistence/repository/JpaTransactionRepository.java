package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.port.out.TransactionRepository;
import com.agentbanking.ledger.infrastructure.persistence.entity.TransactionEntity;
import com.agentbanking.ledger.infrastructure.persistence.mapper.TransactionMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaTransactionRepository implements TransactionRepository {
    
    private final TransactionJpaRepository jpaRepository;
    
    public JpaTransactionRepository(TransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public TransactionRecord save(TransactionRecord record) {
        TransactionEntity entity = TransactionMapper.toEntity(record);
        if (entity.getTransactionId() == null) {
            entity.setTransactionId(UUID.randomUUID());
        }
        TransactionEntity saved = jpaRepository.save(entity);
        return TransactionMapper.toRecord(saved);
    }
    
    @Override
    public TransactionRecord findByIdempotencyKey(String idempotencyKey) {
        Optional<TransactionEntity> entity = jpaRepository.findByIdempotencyKey(idempotencyKey);
        return entity.map(TransactionMapper::toRecord).orElse(null);
    }
    
    @Override
    public TransactionRecord findById(UUID transactionId) {
        Optional<TransactionEntity> entity = jpaRepository.findById(transactionId);
        return entity.map(TransactionMapper::toRecord).orElse(null);
    }
}

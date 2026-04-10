package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.model.TransactionStatus;
import com.agentbanking.ledger.domain.model.TransactionType;
import com.agentbanking.ledger.domain.port.out.TransactionRepository;
import com.agentbanking.ledger.infrastructure.persistence.entity.TransactionEntity;
import com.agentbanking.ledger.infrastructure.persistence.mapper.TransactionMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@org.springframework.context.annotation.Primary
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
    
    @Override
    public List<TransactionRecord> findRecentTransactions() {
        List<TransactionEntity> entities = jpaRepository.findRecentTransactions();
        List<TransactionRecord> records = new ArrayList<>();
        for (TransactionEntity entity : entities) {
            records.add(TransactionMapper.toRecord(entity));
        }
        return records;
    }
    
    @Override
    public BigDecimal sumSuccessfulTransactionAmountByType(TransactionType type) {
        BigDecimal sum = jpaRepository.sumSuccessfulTransactionAmountByType(type);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumSuccessfulTransactionAmount() {
        return jpaRepository.sumSuccessfulTransactionAmount();
    }
    
    @Override
    public long countAllTransactions() {
        return jpaRepository.countAllTransactions();
    }
    
    @Override
    public long countDistinctAgents() {
        return jpaRepository.countDistinctAgents();
    }

    @Override
    public long countByAgentIdAndStatus(UUID agentId, TransactionStatus status) {
        return jpaRepository.countByAgentIdAndStatus(agentId, status);
    }

    @Override
    public boolean existsByAgentIdAndStatusIn(UUID agentId, List<TransactionStatus> statuses) {
        return jpaRepository.existsByAgentIdAndStatusIn(agentId, statuses);
    }

    @Override
    public List<UUID> findAgentIdsWithTransactionsOnDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        return jpaRepository.findDistinctAgentIdsByCompletedAtBetween(startOfDay, endOfDay);
    }

    @Override
    public List<TransactionRecord> findByAgentIdAndCompletedDate(UUID agentId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<TransactionEntity> entities = jpaRepository.findByAgentIdAndCompletedAtBetween(agentId, startOfDay, endOfDay);
        List<TransactionRecord> records = new ArrayList<>();
        for (TransactionEntity entity : entities) {
            records.add(TransactionMapper.toRecord(entity));
        }
        return records;
    }

    @Override
    public List<TransactionRecord> findByAgentId(UUID agentId) {
        List<TransactionEntity> entities = jpaRepository.findByAgentIdOrderByCreatedAtDesc(agentId);
        List<TransactionRecord> records = new ArrayList<>();
        for (TransactionEntity entity : entities) {
            records.add(TransactionMapper.toRecord(entity));
        }
        return records;
    }

    @Override
    public List<TransactionRecord> findByAgentIdAndStatus(UUID agentId, TransactionStatus status) {
        List<TransactionEntity> entities = jpaRepository.findByAgentIdAndStatus(agentId, status);
        List<TransactionRecord> records = new ArrayList<>();
        for (TransactionEntity entity : entities) {
            records.add(TransactionMapper.toRecord(entity));
        }
        return records;
    }
}

package com.agentbanking.ledger.infrastructure.persistence.adapter;

import com.agentbanking.common.transaction.TransactionStatus;
import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.port.out.TransactionRepository;
import com.agentbanking.ledger.infrastructure.persistence.mapper.TransactionMapper;
import com.agentbanking.ledger.infrastructure.persistence.repository.TransactionJpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;

    public TransactionRepositoryAdapter(TransactionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TransactionRecord save(TransactionRecord record) {
        var entity = TransactionMapper.toEntity(record);
        var saved = jpaRepository.save(entity);
        return TransactionMapper.toRecord(saved);
    }

    @Override
    public TransactionRecord findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey)
                .map(TransactionMapper::toRecord)
                .orElse(null);
    }

    @Override
    public TransactionRecord findById(UUID transactionId) {
        return jpaRepository.findById(transactionId)
                .map(TransactionMapper::toRecord)
                .orElse(null);
    }

    @Override
    public List<TransactionRecord> findRecentTransactions() {
        return jpaRepository.findRecentTransactions().stream()
                .map(TransactionMapper::toRecord)
                .toList();
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
        return jpaRepository.findByAgentIdAndCompletedAtBetween(agentId, startOfDay, endOfDay)
                .stream()
                .map(TransactionMapper::toRecord)
                .toList();
    }
}
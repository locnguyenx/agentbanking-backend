package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.model.SettlementSummaryRecord;
import com.agentbanking.ledger.domain.port.out.SettlementSummaryRepository;
import com.agentbanking.ledger.infrastructure.persistence.mapper.SettlementSummaryMapper;
import com.agentbanking.ledger.infrastructure.persistence.repository.SettlementSummaryJpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class SettlementSummaryRepositoryAdapter implements SettlementSummaryRepository {

    private final SettlementSummaryJpaRepository jpaRepository;

    public SettlementSummaryRepositoryAdapter(SettlementSummaryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SettlementSummaryRecord save(SettlementSummaryRecord record) {
        var entity = SettlementSummaryMapper.toEntity(record);
        var saved = jpaRepository.save(entity);
        return SettlementSummaryMapper.toRecord(saved);
    }

    @Override
    public List<SettlementSummaryRecord> findBySettlementDate(LocalDate date) {
        return jpaRepository.findBySettlementDate(date).stream()
                .map(SettlementSummaryMapper::toRecord)
                .toList();
    }

    @Override
    public SettlementSummaryRecord findByAgentIdAndDate(UUID agentId, LocalDate date) {
        return jpaRepository.findByAgentIdAndSettlementDate(agentId, date)
                .map(SettlementSummaryMapper::toRecord)
                .orElse(null);
    }
}

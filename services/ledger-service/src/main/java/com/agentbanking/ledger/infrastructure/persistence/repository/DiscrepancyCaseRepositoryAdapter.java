package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.model.DiscrepancyCase;
import com.agentbanking.ledger.domain.port.out.DiscrepancyCaseRepository;
import com.agentbanking.ledger.infrastructure.persistence.mapper.DiscrepancyCaseMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DiscrepancyCaseRepositoryAdapter implements DiscrepancyCaseRepository {

    private final DiscrepancyCaseJpaRepository jpaRepository;

    public DiscrepancyCaseRepositoryAdapter(DiscrepancyCaseJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DiscrepancyCase save(DiscrepancyCase discrepancyCase) {
        var entity = DiscrepancyCaseMapper.toEntity(discrepancyCase);
        var saved = jpaRepository.save(entity);
        return DiscrepancyCaseMapper.toRecord(saved);
    }

    @Override
    public Optional<DiscrepancyCase> findById(UUID caseId) {
        return jpaRepository.findById(caseId).map(DiscrepancyCaseMapper::toRecord);
    }

    @Override
    public List<DiscrepancyCase> findByStatus(String status) {
        return jpaRepository.findByStatus(com.agentbanking.ledger.domain.model.DiscrepancyStatus.valueOf(status))
                .stream()
                .map(DiscrepancyCaseMapper::toRecord)
                .toList();
    }
}

package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentStatus;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.infrastructure.persistence.entity.AgentEntity;
import com.agentbanking.onboarding.infrastructure.persistence.mapper.AgentMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgentRepositoryImpl implements AgentRepository {

    private final AgentJpaRepository jpaRepository;
    private final AgentMapper agentMapper;
    private final TransactionQueryClient transactionQueryClient;

    public AgentRepositoryImpl(AgentJpaRepository jpaRepository, AgentMapper agentMapper, TransactionQueryClient transactionQueryClient) {
        this.jpaRepository = jpaRepository;
        this.agentMapper = agentMapper;
        this.transactionQueryClient = transactionQueryClient;
    }

    @Override
    public AgentRecord save(AgentRecord agent) {
        AgentEntity entity = agentMapper.toEntity(agent);
        AgentEntity saved = jpaRepository.save(entity);
        return agentMapper.toRecord(saved);
    }

    @Override
    public Optional<AgentRecord> findById(UUID agentId) {
        return jpaRepository.findById(agentId).map(agentMapper::toRecord);
    }

    @Override
    public Optional<AgentRecord> findByMykadNumber(String mykadNumber) {
        return jpaRepository.findByMykadNumber(mykadNumber).map(agentMapper::toRecord);
    }

    @Override
    public List<AgentRecord> findAll(int page, int size) {
        Page<AgentEntity> result = jpaRepository.findAll(PageRequest.of(page, size));
        return result.getContent().stream().map(agentMapper::toRecord).toList();
    }

    @Override
    public boolean hasPendingTransactions(UUID agentId) {
        return transactionQueryClient.hasPendingTransactions(agentId);
    }

    @Override
    public long countByStatus(AgentStatus status) {
        return jpaRepository.findByStatus(status, PageRequest.of(0, 1)).getTotalElements();
    }
}

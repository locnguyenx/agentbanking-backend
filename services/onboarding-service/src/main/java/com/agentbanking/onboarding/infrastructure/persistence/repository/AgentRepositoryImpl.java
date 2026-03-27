package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentStatus;
import com.agentbanking.onboarding.domain.port.out.AgentRepository;
import com.agentbanking.onboarding.infrastructure.persistence.entity.AgentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AgentRepositoryImpl implements AgentRepository {

    private final AgentJpaRepository jpaRepository;

    public AgentRepositoryImpl(AgentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AgentRecord save(AgentRecord agent) {
        AgentEntity entity = toEntity(agent);
        AgentEntity saved = jpaRepository.save(entity);
        return toRecord(saved);
    }

    @Override
    public Optional<AgentRecord> findById(UUID agentId) {
        return jpaRepository.findById(agentId).map(this::toRecord);
    }

    @Override
    public Optional<AgentRecord> findByMykadNumber(String mykadNumber) {
        return jpaRepository.findByMykadNumber(mykadNumber).map(this::toRecord);
    }

    @Override
    public List<AgentRecord> findAll(int page, int size) {
        Page<AgentEntity> result = jpaRepository.findAll(PageRequest.of(page, size));
        return result.getContent().stream().map(this::toRecord).toList();
    }

    @Override
    public boolean hasPendingTransactions(UUID agentId) {
        return false;
    }

    @Override
    public long countByStatus(AgentStatus status) {
        return jpaRepository.findByStatus(status, PageRequest.of(0, 1)).getTotalElements();
    }

    private AgentRecord toRecord(AgentEntity entity) {
        return new AgentRecord(
            entity.getAgentId(),
            entity.getAgentCode(),
            entity.getBusinessName(),
            entity.getTier(),
            entity.getStatus(),
            entity.getMerchantGpsLat(),
            entity.getMerchantGpsLng(),
            entity.getMykadNumber(),
            entity.getPhoneNumber(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    private AgentEntity toEntity(AgentRecord record) {
        AgentEntity entity = new AgentEntity();
        entity.setAgentId(record.agentId());
        entity.setAgentCode(record.agentCode());
        entity.setBusinessName(record.businessName());
        entity.setTier(record.tier());
        entity.setStatus(record.status());
        entity.setMerchantGpsLat(record.merchantGpsLat());
        entity.setMerchantGpsLng(record.merchantGpsLng());
        entity.setMykadNumber(record.mykadNumber());
        entity.setPhoneNumber(record.phoneNumber());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }
}

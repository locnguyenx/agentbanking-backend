package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import com.agentbanking.onboarding.domain.port.out.AgentOnboardingRepository;
import com.agentbanking.onboarding.infrastructure.persistence.entity.AgentOnboardingEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of AgentOnboardingRepository
 */
@Repository
public class AgentOnboardingRepositoryImpl implements AgentOnboardingRepository {

    private final AgentOnboardingJpaRepository jpaRepository;

    public AgentOnboardingRepositoryImpl(AgentOnboardingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AgentOnboardingRecord save(AgentOnboardingRecord record) {
        AgentOnboardingEntity entity = AgentOnboardingEntity.fromRecord(record);
        AgentOnboardingEntity saved = jpaRepository.save(entity);
        return saved.toRecord();
    }

    @Override
    public Optional<AgentOnboardingRecord> findById(UUID onboardingId) {
        return jpaRepository.findById(onboardingId).map(AgentOnboardingEntity::toRecord);
    }
}
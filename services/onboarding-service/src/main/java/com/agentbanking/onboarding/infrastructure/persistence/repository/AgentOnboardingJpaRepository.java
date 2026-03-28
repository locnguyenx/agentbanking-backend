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
public class AgentOnboardingJpaRepository implements AgentOnboardingRepository {

    @Override
    public AgentOnboardingRecord save(AgentOnboardingRecord record) {
        AgentOnboardingEntity entity = AgentOnboardingEntity.fromRecord(record);
        AgentOnboardingEntity saved = saveEntity(entity);
        return saved.toRecord();
    }

    @Override
    public Optional<AgentOnboardingRecord> findById(UUID onboardingId) {
        // This would delegate to a Spring Data JPA repository
        // For now, we'll simulate the operation
        // In a real implementation, this would be: return agentOnboardingJpaRepository.findById(onboardingId).map(AgentOnboardingEntity::toRecord);
        return Optional.empty(); // Simplified for this example
    }

    private AgentOnboardingEntity saveEntity(AgentOnboardingEntity entity) {
        // This would typically delegate to a Spring Data JPA repository
        // For now, we'll simulate the save operation
        // In a real implementation, this would be: return agentOnboardingJpaRepository.save(entity);
        return entity; // Simplified for this example
    }
}
package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.domain.model.OnboardingDecision;
import com.agentbanking.onboarding.domain.port.out.OnboardingDecisionRepository;
import com.agentbanking.onboarding.infrastructure.persistence.entity.OnboardingDecisionEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of OnboardingDecisionRepository
 */
@Repository
public class OnboardingDecisionJpaRepository implements OnboardingDecisionRepository {

    @Override
    public OnboardingDecision save(OnboardingDecision decision) {
        OnboardingDecisionEntity entity = OnboardingDecisionEntity.fromRecord(decision);
        OnboardingDecisionEntity saved = saveEntity(entity);
        return saved.toRecord();
    }

    @Override
    public Optional<OnboardingDecision> findById(UUID decisionId) {
        // This would delegate to a Spring Data JPA repository
        // For now, we'll simulate the operation
        // In a real implementation, this would be: return onboardingDecisionJpaRepository.findById(decisionId).map(OnboardingDecisionEntity::toRecord);
        return Optional.empty(); // Simplified for this example
    }

    private OnboardingDecisionEntity saveEntity(OnboardingDecisionEntity entity) {
        // This would typically delegate to a Spring Data JPA repository
        // For now, we'll simulate the save operation
        // In a real implementation, this would be: return onboardingDecisionJpaRepository.save(entity);
        return entity; // Simplified for this example
    }
}
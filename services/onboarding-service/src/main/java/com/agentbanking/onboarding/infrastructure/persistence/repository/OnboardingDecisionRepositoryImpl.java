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
public class OnboardingDecisionRepositoryImpl implements OnboardingDecisionRepository {

    private final OnboardingDecisionJpaRepository jpaRepository;

    public OnboardingDecisionRepositoryImpl(OnboardingDecisionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OnboardingDecision save(OnboardingDecision decision) {
        OnboardingDecisionEntity entity = OnboardingDecisionEntity.fromRecord(decision);
        OnboardingDecisionEntity saved = jpaRepository.save(entity);
        return saved.toRecord();
    }

    @Override
    public Optional<OnboardingDecision> findById(UUID decisionId) {
        return jpaRepository.findById(decisionId).map(OnboardingDecisionEntity::toRecord);
    }
}
package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.infrastructure.persistence.entity.OnboardingDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for OnboardingDecisionEntity
 */
@Repository
public interface OnboardingDecisionJpaRepository extends JpaRepository<OnboardingDecisionEntity, UUID> {
}

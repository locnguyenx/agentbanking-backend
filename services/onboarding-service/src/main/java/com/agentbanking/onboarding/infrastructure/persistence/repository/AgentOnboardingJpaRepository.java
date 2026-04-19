package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.infrastructure.persistence.entity.AgentOnboardingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for AgentOnboardingEntity
 */
@Repository
public interface AgentOnboardingJpaRepository extends JpaRepository<AgentOnboardingEntity, UUID> {
}

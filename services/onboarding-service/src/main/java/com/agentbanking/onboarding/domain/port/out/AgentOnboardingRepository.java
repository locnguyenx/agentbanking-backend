package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for agent onboarding records
 */
public interface AgentOnboardingRepository {
    AgentOnboardingRecord save(AgentOnboardingRecord record);
    Optional<AgentOnboardingRecord> findById(UUID onboardingId);
}
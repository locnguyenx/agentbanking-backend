package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;

/**
 * Use case for starting Standard/Premier agent onboarding (Non-STP)
 */
public interface StartStandardPremierOnboardingUseCase {
    AgentOnboardingRecord start(String mykadNumber, String agentTier);
}

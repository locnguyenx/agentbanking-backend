package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;

/**
 * Use case for starting a micro-agent onboarding application (Conditional STP)
 */
public interface StartMicroAgentOnboardingUseCase {
    AgentOnboardingRecord start(String mykadNumber);
}
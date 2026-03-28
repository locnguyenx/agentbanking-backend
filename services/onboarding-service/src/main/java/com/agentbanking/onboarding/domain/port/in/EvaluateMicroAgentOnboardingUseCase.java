package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import com.agentbanking.onboarding.domain.model.OnboardingDecision;
import java.util.UUID;

/**
 * Use case for evaluating a micro-agent onboarding application (Conditional STP)
 */
public interface EvaluateMicroAgentOnboardingUseCase {
    OnboardingDecision evaluate(UUID onboardingId);
}
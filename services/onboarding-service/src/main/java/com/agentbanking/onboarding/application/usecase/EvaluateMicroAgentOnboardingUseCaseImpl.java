package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.OnboardingDecision;
import com.agentbanking.onboarding.domain.port.in.EvaluateMicroAgentOnboardingUseCase;
import com.agentbanking.onboarding.domain.service.AgentOnboardingService;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementation of EvaluateMicroAgentOnboardingUseCase
 */
@Component
public class EvaluateMicroAgentOnboardingUseCaseImpl implements EvaluateMicroAgentOnboardingUseCase {

    private final AgentOnboardingService agentOnboardingService;

    public EvaluateMicroAgentOnboardingUseCaseImpl(AgentOnboardingService agentOnboardingService) {
        this.agentOnboardingService = agentOnboardingService;
    }

    @Override
    public OnboardingDecision evaluate(UUID onboardingId) {
        return agentOnboardingService.evaluateMicroAgentOnboarding(onboardingId);
    }
}
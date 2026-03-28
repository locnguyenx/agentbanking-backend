package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import com.agentbanking.onboarding.domain.port.in.StartMicroAgentOnboardingUseCase;
import com.agentbanking.onboarding.domain.service.AgentOnboardingService;
import org.springframework.stereotype.Component;

/**
 * Implementation of StartMicroAgentOnboardingUseCase
 */
@Component
public class StartMicroAgentOnboardingUseCaseImpl implements StartMicroAgentOnboardingUseCase {

    private final AgentOnboardingService agentOnboardingService;

    public StartMicroAgentOnboardingUseCaseImpl(AgentOnboardingService agentOnboardingService) {
        this.agentOnboardingService = agentOnboardingService;
    }

    @Override
    public AgentOnboardingRecord start(String mykadNumber) {
        return agentOnboardingService.startMicroAgentOnboarding(mykadNumber);
    }
}
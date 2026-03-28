package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import com.agentbanking.onboarding.domain.port.in.StartStandardPremierOnboardingUseCase;
import com.agentbanking.onboarding.domain.service.AgentOnboardingService;
import org.springframework.stereotype.Component;

/**
 * Implementation of StartStandardPremierOnboardingUseCase
 */
@Component
public class StartStandardPremierOnboardingUseCaseImpl implements StartStandardPremierOnboardingUseCase {

    private final AgentOnboardingService agentOnboardingService;

    public StartStandardPremierOnboardingUseCaseImpl(AgentOnboardingService agentOnboardingService) {
        this.agentOnboardingService = agentOnboardingService;
    }

    @Override
    public AgentOnboardingRecord start(String mykadNumber, String agentTier) {
        return agentOnboardingService.startStandardPremierOnboarding(mykadNumber, agentTier);
    }
}
package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.port.in.DeactivateAgentUseCase;
import com.agentbanking.onboarding.domain.service.AgentService;

import java.util.UUID;

public class DeactivateAgentUseCaseImpl implements DeactivateAgentUseCase {

    private final AgentService agentService;

    public DeactivateAgentUseCaseImpl(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public AgentRecord deactivate(UUID agentId) {
        return agentService.deactivateAgent(agentId);
    }
}

package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.port.in.UpdateAgentUseCase;
import com.agentbanking.onboarding.domain.service.AgentService;

import java.util.UUID;

public class UpdateAgentUseCaseImpl implements UpdateAgentUseCase {

    private final AgentService agentService;

    public UpdateAgentUseCaseImpl(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public AgentRecord update(UUID agentId, UpdateAgentCommand command) {
        return agentService.updateAgent(agentId, command);
    }
}

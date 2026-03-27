package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.port.in.CreateAgentUseCase;
import com.agentbanking.onboarding.domain.service.AgentService;
import org.springframework.transaction.annotation.Transactional;

public class CreateAgentUseCaseImpl implements CreateAgentUseCase {

    private final AgentService agentService;

    public CreateAgentUseCaseImpl(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    @Transactional
    public AgentRecord create(CreateAgentCommand command) {
        return agentService.createAgent(command);
    }
}

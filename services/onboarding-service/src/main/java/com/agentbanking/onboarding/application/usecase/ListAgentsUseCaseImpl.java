package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.port.in.ListAgentsUseCase;
import com.agentbanking.onboarding.domain.service.AgentService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ListAgentsUseCaseImpl implements ListAgentsUseCase {

    private final AgentService agentService;

    public ListAgentsUseCaseImpl(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public List<AgentRecord> list(int page, int size) {
        return agentService.listAgents(page, size);
    }

    @Override
    public Optional<AgentRecord> findById(UUID agentId) {
        return agentService.findById(agentId);
    }
}

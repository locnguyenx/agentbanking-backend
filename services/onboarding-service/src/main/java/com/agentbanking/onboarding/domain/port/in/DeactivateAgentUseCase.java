package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.AgentRecord;

import java.util.UUID;

public interface DeactivateAgentUseCase {
    AgentRecord deactivate(UUID agentId);
}

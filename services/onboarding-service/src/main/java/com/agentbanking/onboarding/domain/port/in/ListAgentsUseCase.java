package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.AgentRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListAgentsUseCase {
    List<AgentRecord> list(int page, int size);
    Optional<AgentRecord> findById(UUID agentId);
}

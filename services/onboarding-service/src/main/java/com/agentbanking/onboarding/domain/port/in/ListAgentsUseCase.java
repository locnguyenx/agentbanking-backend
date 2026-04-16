package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListAgentsUseCase {
    List<AgentRecord> list(int page, int size);
    long countAll();
    long countByStatus(AgentStatus status);
    Optional<AgentRecord> findById(UUID agentId);
}

package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentRepository {
    AgentRecord save(AgentRecord agent);
    Optional<AgentRecord> findById(UUID agentId);
    Optional<AgentRecord> findByMykadNumber(String mykadNumber);
    List<AgentRecord> findAll(int page, int size);
    long countAll();
    boolean hasPendingTransactions(UUID agentId);
    long countByStatus(AgentStatus status);
}

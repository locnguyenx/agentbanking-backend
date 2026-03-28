package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;
import com.agentbanking.onboarding.domain.model.AgentRecord;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for Agent data
 * Queries agent information needed for transaction processing
 */
public interface AgentRepository {
    Optional<AgentRecord> findById(UUID agentId);
    Optional<AgentFloatRecord> findAgentFloat(UUID agentId);
    boolean hasPendingTransactions(UUID agentId);
}

package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository port for Agent data
 * Queries agent information needed for transaction processing
 */
public interface AgentRepository {
    Optional<AgentFloatRecord> findAgentFloat(UUID agentId);
    boolean hasPendingTransactions(UUID agentId);
}

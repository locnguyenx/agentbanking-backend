package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;

import java.util.UUID;

public interface AgentFloatRepository {
    AgentFloatRecord findByIdWithLock(UUID agentId);
    AgentFloatRecord save(AgentFloatRecord record);
}

package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;

import java.math.BigDecimal;
import java.util.UUID;

public interface AgentFloatRepository {
    AgentFloatRecord findByIdWithLock(UUID agentId);
    AgentFloatRecord findById(UUID agentId);
    AgentFloatRecord save(AgentFloatRecord record);
    void updateBalance(UUID agentId, BigDecimal balanceChange);
}

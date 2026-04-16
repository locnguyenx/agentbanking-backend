package com.agentbanking.ledger.domain.port.in;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;

import java.math.BigDecimal;
import java.util.UUID;

public interface GetAgentFloatUseCase {
    AgentFloatRecord getAgentFloat(UUID agentId);
    boolean exists(UUID agentId);
}
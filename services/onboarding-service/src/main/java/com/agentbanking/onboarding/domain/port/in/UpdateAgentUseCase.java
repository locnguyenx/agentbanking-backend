package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentTier;

import java.math.BigDecimal;
import java.util.UUID;

public interface UpdateAgentUseCase {
    AgentRecord update(UUID agentId, UpdateAgentCommand command);

    record UpdateAgentCommand(
        String businessName,
        AgentTier tier,
        BigDecimal merchantGpsLat,
        BigDecimal merchantGpsLng,
        String phoneNumber
    ) {}
}

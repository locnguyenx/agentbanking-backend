package com.agentbanking.onboarding.domain.port.in;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentTier;

import java.math.BigDecimal;

public interface CreateAgentUseCase {
    AgentRecord create(CreateAgentCommand command);

    record CreateAgentCommand(
        String agentCode,
        String businessName,
        AgentTier tier,
        BigDecimal merchantGpsLat,
        BigDecimal merchantGpsLng,
        String mykadNumber,
        String phoneNumber
    ) {}
}

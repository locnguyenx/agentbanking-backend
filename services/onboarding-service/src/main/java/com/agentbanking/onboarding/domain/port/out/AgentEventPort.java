package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.common.event.AgentCreatedEvent;

public interface AgentEventPort {
    void publishAgentCreated(AgentCreatedEvent event);
}
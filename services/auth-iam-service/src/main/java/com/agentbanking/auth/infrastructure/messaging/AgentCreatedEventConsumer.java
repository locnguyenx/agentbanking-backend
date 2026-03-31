package com.agentbanking.auth.infrastructure.messaging;

import com.agentbanking.auth.domain.model.AgentCreatedEvent;
import com.agentbanking.auth.domain.service.AgentUserSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class AgentCreatedEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(AgentCreatedEventConsumer.class);
    
    private final AgentUserSyncService agentUserSyncService;

    public AgentCreatedEventConsumer(AgentUserSyncService agentUserSyncService) {
        this.agentUserSyncService = agentUserSyncService;
    }

    @Bean
    public Consumer<AgentCreatedEvent> agentCreatedIn() {
        return event -> {
            log.info("Received AGENT_CREATED event for agent: {}", event.data().agentCode());
            agentUserSyncService.handleAgentCreated(event);
        };
    }
}

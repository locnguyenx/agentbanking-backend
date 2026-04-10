package com.agentbanking.onboarding.infrastructure.messaging;

import com.agentbanking.common.event.AgentCreatedEvent;
import com.agentbanking.common.messaging.KafkaTopics;
import com.agentbanking.onboarding.domain.port.out.AgentEventPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class AgentEventPublisher implements AgentEventPort {

    private static final Logger log = LoggerFactory.getLogger(AgentEventPublisher.class);
    private final StreamBridge streamBridge;

    public AgentEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public void publishAgentCreated(AgentCreatedEvent event) {
        log.info("Publishing AGENT_CREATED event for agent: {}", event.data().agentId());
        try {
            boolean sent = streamBridge.send("agentCreated-out-0", event);
            if (sent) {
                log.info("Successfully published AGENT_CREATED event for agent: {}", event.data().agentId());
            } else {
                log.error("Failed to publish AGENT_CREATED event for agent: {} - binding may not be ready", event.data().agentId());
            }
        } catch (Exception e) {
            log.error("Exception publishing AGENT_CREATED event for agent: {}", event.data().agentId(), e);
        }
    }
}
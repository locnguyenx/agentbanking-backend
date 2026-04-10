package com.agentbanking.ledger.infrastructure.messaging;

import com.agentbanking.common.event.AgentCreatedEvent;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class LedgerEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LedgerEventConsumer.class);
    private final LedgerService ledgerService;

    public LedgerEventConsumer(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @Bean
    public Consumer<AgentCreatedEvent> agentCreatedIn() {
        return event -> {
            log.info("Received AGENT_CREATED event for agent: {}", event.data().agentId());
            try {
                ledgerService.provisionAgentFloat(
                    event.data().agentId(),
                    event.data().tier(),
                    event.data().merchantGpsLat() != null ? event.data().merchantGpsLat() : 0.0,
                    event.data().merchantGpsLng() != null ? event.data().merchantGpsLng() : 0.0,
                    null, // description
                    null, // referenceNumber
                    null, // billerCode
                    null, // targetBin
                    null, // destinationAccount
                    null, // ref1
                    null  // ref2
                );
                log.info("Successfully provisioned agent float for agent: {}", event.data().agentId());
            } catch (Exception e) {
                log.error("Failed to provision agent float for agent: {}", event.data().agentId(), e);
            }
        };
    }
}
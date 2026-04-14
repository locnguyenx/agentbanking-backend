package com.agentbanking.auth.infrastructure.messaging;

import com.agentbanking.auth.domain.model.AgentCreatedEvent;
import com.agentbanking.auth.domain.service.AgentUserSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AgentCreatedEventConsumerTest {

    @Mock
    private AgentUserSyncService agentUserSyncService;

    private AgentCreatedEventConsumer consumer;
    private Consumer<AgentCreatedEvent> agentCreatedIn;

    @BeforeEach
    void setUp() {
        consumer = new AgentCreatedEventConsumer(agentUserSyncService);
        agentCreatedIn = consumer.agentCreatedIn();
    }

    @Test
    void agentCreatedIn_withValidEvent_callsSyncService() {
        UUID agentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        AgentCreatedEvent event = new AgentCreatedEvent(
            eventId,
            "AGENT_CREATED",
            Instant.now(),
            new AgentCreatedEvent.AgentCreatedData(
                agentId,
                "TEST-001",
                "+60123456789",
                "test@example.com",
                "Test Business"
            )
        );

        doNothing().when(agentUserSyncService).handleAgentCreated(event);

        agentCreatedIn.accept(event);

        verify(agentUserSyncService).handleAgentCreated(event);
    }

    @Test
    void agentCreatedIn_withNoPhone_stillCallsSyncService() {
        UUID agentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        AgentCreatedEvent event = new AgentCreatedEvent(
            eventId,
            "AGENT_CREATED",
            Instant.now(),
            new AgentCreatedEvent.AgentCreatedData(
                agentId,
                "TEST-002",
                null,
                "test@example.com",
                "Test Business"
            )
        );

        doNothing().when(agentUserSyncService).handleAgentCreated(event);

        agentCreatedIn.accept(event);

        verify(agentUserSyncService).handleAgentCreated(event);
    }

    @Test
    void agentCreatedIn_withException_propagatesException() {
        UUID agentId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        AgentCreatedEvent event = new AgentCreatedEvent(
            eventId,
            "AGENT_CREATED",
            Instant.now(),
            new AgentCreatedEvent.AgentCreatedData(
                agentId,
                "TEST-003",
                "+60123456789",
                "test@example.com",
                "Test Business"
            )
        );

        doThrow(new RuntimeException("Service error"))
            .when(agentUserSyncService).handleAgentCreated(event);

        try {
            agentCreatedIn.accept(event);
        } catch (RuntimeException e) {
            assertEquals("Service error", e.getMessage());
        }

        verify(agentUserSyncService).handleAgentCreated(event);
    }
}
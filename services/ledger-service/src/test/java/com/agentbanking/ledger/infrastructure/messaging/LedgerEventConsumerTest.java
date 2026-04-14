package com.agentbanking.ledger.infrastructure.messaging;

import com.agentbanking.common.event.AgentCreatedEvent;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerEventConsumerTest {

    @Mock
    private LedgerService ledgerService;

    private LedgerEventConsumer consumer;
    private Consumer<AgentCreatedEvent> agentCreatedIn;

    @BeforeEach
    void setUp() {
        consumer = new LedgerEventConsumer(ledgerService);
        agentCreatedIn = consumer.agentCreatedIn();
    }

    @Test
    void agentCreatedIn_withValidEvent_provisionsAgentFloat() {
        UUID agentId = UUID.randomUUID();
        AgentCreatedEvent event = AgentCreatedEvent.create(
            agentId,
            "TEST-001",
            "+60123456789",
            "test@example.com",
            "Test Business",
            "STANDARD",
            3.139,
            101.686
        );

        agentCreatedIn.accept(event);

        verify(ledgerService).provisionAgentFloat(
            eq(agentId),
            eq("STANDARD"),
            eq(3.139),
            eq(101.686),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull()
        );
    }

    @Test
    void agentCreatedIn_withNullGps_usesDefaultValues() {
        UUID agentId = UUID.randomUUID();
        AgentCreatedEvent event = AgentCreatedEvent.create(
            agentId,
            "TEST-002",
            "+60123456789",
            "test@example.com",
            "Test Business",
            "MICRO",
            null,
            null
        );

        agentCreatedIn.accept(event);

        verify(ledgerService).provisionAgentFloat(
            eq(agentId),
            eq("MICRO"),
            eq(0.0),
            eq(0.0),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull()
        );
    }

    @Test
    void agentCreatedIn_withFailure_logsError() {
        UUID agentId = UUID.randomUUID();
        AgentCreatedEvent event = AgentCreatedEvent.create(
            agentId,
            "TEST-003",
            "+60123456789",
            "test@example.com",
            "Test Business",
            "STANDARD",
            3.139,
            101.686
        );

        doThrow(new RuntimeException("Database error"))
            .when(ledgerService).provisionAgentFloat(any(), any(), anyDouble(), anyDouble(), any(), any(), any(), any(), any(), any(), any());

        agentCreatedIn.accept(event);

        verify(ledgerService).provisionAgentFloat(
            eq(agentId),
            eq("STANDARD"),
            eq(3.139),
            eq(101.686),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull()
        );
    }

    @Test
    void agentCreatedIn_withExistingAgent_stillProvisionsFloat() {
        UUID agentId = UUID.randomUUID();
        AgentCreatedEvent event = AgentCreatedEvent.create(
            agentId,
            "TEST-004",
            "+60123456789",
            "test@example.com",
            "Test Business",
            "PREMIER",
            3.139,
            101.686
        );

        doNothing().when(ledgerService).provisionAgentFloat(any(), any(), anyDouble(), anyDouble(), any(), any(), any(), any(), any(), any(), any());

        agentCreatedIn.accept(event);

        verify(ledgerService).provisionAgentFloat(
            eq(agentId),
            eq("PREMIER"),
            eq(3.139),
            eq(101.686),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull()
        );
    }
}
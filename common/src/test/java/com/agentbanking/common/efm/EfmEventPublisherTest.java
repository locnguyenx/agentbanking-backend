package com.agentbanking.common.efm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EfmEventPublisherTest {

    private EfmEventPublisher efmEventPublisher;

    @BeforeEach
    void setUp() {
        efmEventPublisher = new EfmEventPublisher();
    }

    @Test
    void publishEvent_doesNotThrow() {
        UUID txId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        Map<String, Object> details = Map.of("amount", "500.00", "type", "WITHDRAWAL");

        assertDoesNotThrow(() -> efmEventPublisher.publishEvent("TRANSACTION", txId, agentId, details));
    }

    @Test
    void publishFraudAlert_doesNotThrow() {
        UUID txId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        assertDoesNotThrow(() -> efmEventPublisher.publishFraudAlert("GPS_UNAVAILABLE", txId, agentId, "GPS signal lost"));
    }

    @Test
    void publishEvent_withNullTransactionId_doesNotThrow() {
        assertDoesNotThrow(() -> efmEventPublisher.publishEvent("TRANSACTION", null, UUID.randomUUID(), Map.of()));
    }

    @Test
    void publishFraudAlert_withNullAgentId_doesNotThrow() {
        assertDoesNotThrow(() -> efmEventPublisher.publishFraudAlert("VELOCITY", UUID.randomUUID(), null, "exceeded"));
    }

    @Test
    void efmEvent_recordHasCorrectFields() {
        UUID eventId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        Map<String, Object> details = Map.of("key", "value");
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        EfmEventPublisher.EfmEvent event = new EfmEventPublisher.EfmEvent(
                eventId, "TEST", txId, agentId, details, now
        );

        assertEquals(eventId, event.eventId());
        assertEquals("TEST", event.eventType());
        assertEquals(txId, event.transactionId());
        assertEquals(agentId, event.agentId());
        assertEquals(details, event.details());
        assertEquals(now, event.timestamp());
    }
}

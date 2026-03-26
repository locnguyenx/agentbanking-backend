package com.agentbanking.ledger.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReversalEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ReversalEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ReversalEventPublisher(kafkaTemplate);
    }

    @Test
    void publishReversalEvent_sendsToCorrectTopic() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        UUID originalTransactionId = UUID.randomUUID();
        UUID reversalTransactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        ReversalEvent event = new ReversalEvent(
            UUID.randomUUID(),
            "TRANSACTION_REVERSAL",
            originalTransactionId,
            reversalTransactionId,
            agentId,
            new BigDecimal("1000.00"),
            "MYR",
            "CASH_WITHDRAWAL",
            "CUSTOMER_REQUEST",
            LocalDateTime.now()
        );

        publisher.publish(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ReversalEvent> eventCaptor = ArgumentCaptor.forClass(ReversalEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertEquals("ledger.reversals", topicCaptor.getValue());
        assertEquals(agentId.toString(), keyCaptor.getValue());
        assertEquals(event, eventCaptor.getValue());
    }

    @Test
    void publishReversalEvent_withCorrectEventFields() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        UUID eventId = UUID.randomUUID();
        UUID originalTransactionId = UUID.randomUUID();
        UUID reversalTransactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.now();

        ReversalEvent event = new ReversalEvent(
            eventId,
            "TRANSACTION_REVERSAL",
            originalTransactionId,
            reversalTransactionId,
            agentId,
            new BigDecimal("1000.00"),
            "MYR",
            "CASH_WITHDRAWAL",
            "CUSTOMER_REQUEST",
            timestamp
        );

        publisher.publish(event);

        assertEquals(eventId, event.eventId());
        assertEquals("TRANSACTION_REVERSAL", event.eventType());
        assertEquals(originalTransactionId, event.originalTransactionId());
        assertEquals(reversalTransactionId, event.reversalTransactionId());
        assertEquals(agentId, event.agentId());
        assertEquals(new BigDecimal("1000.00"), event.amount());
        assertEquals("MYR", event.currency());
        assertEquals("CASH_WITHDRAWAL", event.originalTransactionType());
        assertEquals("CUSTOMER_REQUEST", event.reason());
        assertEquals(timestamp, event.timestamp());
    }

    @Test
    void publishReversalEvent_differentReasons() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        String[] reasons = {"CUSTOMER_REQUEST", "SYSTEM_ERROR", "FRAUD_DETECTED", "TIMEOUT"};

        for (String reason : reasons) {
            ReversalEvent event = new ReversalEvent(
                UUID.randomUUID(),
                "TRANSACTION_REVERSAL",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("500.00"),
                "MYR",
                "CASH_DEPOSIT",
                reason,
                LocalDateTime.now()
            );

            publisher.publish(event);

            assertEquals(reason, event.reason());
        }

        verify(kafkaTemplate, times(4)).send(anyString(), anyString(), any());
    }
}

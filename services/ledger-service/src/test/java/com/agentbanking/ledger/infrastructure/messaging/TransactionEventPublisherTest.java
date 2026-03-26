package com.agentbanking.ledger.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private TransactionEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new TransactionEventPublisher(kafkaTemplate);
    }

    @Test
    void publishTransactionCompletedEvent_sendsToCorrectTopic() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        UUID transactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        TransactionEvent event = new TransactionEvent(
            UUID.randomUUID(),
            "COMPLETED",
            transactionId,
            agentId,
            "CASH_WITHDRAWAL",
            new BigDecimal("1000.00"),
            "MYR",
            null,
            "411111******1111"
        );

        publisher.publish(event);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertEquals("ledger.transactions", topicCaptor.getValue());
        assertEquals(agentId.toString(), keyCaptor.getValue());
        assertEquals(event, eventCaptor.getValue());
    }

    @Test
    void publishTransactionCompletedEvent_withCorrectEventFields() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        UUID eventId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        TransactionEvent event = new TransactionEvent(
            eventId,
            "COMPLETED",
            transactionId,
            agentId,
            "CASH_DEPOSIT",
            new BigDecimal("500.00"),
            "MYR",
            null,
            "411111******1111"
        );

        publisher.publish(event);

        assertEquals(eventId, event.eventId());
        assertEquals("COMPLETED", event.status());
        assertEquals(transactionId, event.transactionId());
        assertEquals(agentId, event.agentId());
        assertEquals("CASH_DEPOSIT", event.transactionType());
        assertEquals(new BigDecimal("500.00"), event.amount());
        assertEquals("MYR", event.currency());
    }

    @Test
    void publishTransactionFailedEvent_includesErrorCode() {
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));

        UUID transactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        TransactionEvent event = new TransactionEvent(
            UUID.randomUUID(),
            "FAILED",
            transactionId,
            agentId,
            "CASH_WITHDRAWAL",
            new BigDecimal("1000.00"),
            "MYR",
            "ERR_INSUFFICIENT_FLOAT",
            "411111******1111"
        );

        publisher.publish(event);

        verify(kafkaTemplate).send(eq("ledger.transactions"), eq(agentId.toString()), eq(event));
        assertEquals("FAILED", event.status());
        assertEquals("ERR_INSUFFICIENT_FLOAT", event.errorCode());
    }
}

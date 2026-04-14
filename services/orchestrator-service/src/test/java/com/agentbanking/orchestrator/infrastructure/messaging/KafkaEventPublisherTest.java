package com.agentbanking.orchestrator.infrastructure.messaging;

import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionCompletedEvent;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort.TransactionFailedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private KafkaEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaEventPublisher(kafkaTemplate);
    }

    @Test
    void publishTransactionCompleted_sendsToCorrectTopic() {
        UUID transactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        
        TransactionCompletedEvent event = new TransactionCompletedEvent(
            transactionId,
            agentId,
            new BigDecimal("1000.00"),
            new BigDecimal("10.00"),
            new BigDecimal("5.00"),
            new BigDecimal("2.00"),
            "CASH_WITHDRAWAL",
            "411111******1111",
            "SW123456",
            "REF-001"
        );

        when(kafkaTemplate.send(eq("transaction-events"), eq(event)))
            .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishTransactionCompleted(event);

        verify(kafkaTemplate).send(eq("transaction-events"), eq(event));
    }

    @Test
    void publishTransactionCompleted_withAllFields_sendsCorrectPayload() {
        UUID transactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        
        TransactionCompletedEvent event = new TransactionCompletedEvent(
            transactionId,
            agentId,
            new BigDecimal("5000.00"),
            new BigDecimal("50.00"),
            new BigDecimal("25.00"),
            new BigDecimal("10.00"),
            "CASH_DEPOSIT",
            "880101011234",
            "SW789",
            "REF-002"
        );

        when(kafkaTemplate.send(anyString(), any(TransactionCompletedEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishTransactionCompleted(event);

        verify(kafkaTemplate).send(eq("transaction-events"), eq(event));
    }

    @Test
    void publishTransactionFailed_sendsToCorrectTopic() {
        UUID transactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        
        TransactionFailedEvent event = new TransactionFailedEvent(
            transactionId,
            agentId,
            new BigDecimal("1000.00"),
            "CASH_WITHDRAWAL",
            "411111******1111",
            "Insufficient balance"
        );

        when(kafkaTemplate.send(eq("transaction-events"), eq(event)))
            .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishTransactionFailed(event);

        verify(kafkaTemplate).send(eq("transaction-events"), eq(event));
    }

    @Test
    void publishTransactionFailed_withReason_sendsCorrectPayload() {
        UUID transactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        
        TransactionFailedEvent event = new TransactionFailedEvent(
            transactionId,
            agentId,
            new BigDecimal("250.00"),
            "BILL_PAYMENT",
            "880101011234",
            "Biller not found"
        );

        when(kafkaTemplate.send(anyString(), any(TransactionFailedEvent.class)))
            .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishTransactionFailed(event);

        verify(kafkaTemplate).send(eq("transaction-events"), eq(event));
    }
}
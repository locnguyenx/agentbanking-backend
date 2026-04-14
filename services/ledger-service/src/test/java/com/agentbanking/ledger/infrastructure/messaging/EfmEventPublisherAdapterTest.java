package com.agentbanking.ledger.infrastructure.messaging;

import com.agentbanking.common.efm.EfmEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EfmEventPublisherAdapterTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private EfmEventPublisher efmEventPublisher;

    private EfmEventPublisherAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EfmEventPublisherAdapter(kafkaTemplate, efmEventPublisher);
    }

    @Test
    void publishToKafka_sendsToEfmTopic() {
        UUID transactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        Map<String, Object> details = Map.of("amount", "1000.00", "status", "COMPLETED");

        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        adapter.publishToKafka("TRANSACTION", transactionId, agentId, details);

        verify(kafkaTemplate).send(eq("efm.events"), eq(agentId.toString()), any());
    }

    @Test
    void publishToKafka_withNullTransactionId_usesRandomKey() {
        UUID agentId = UUID.randomUUID();
        Map<String, Object> details = Map.of("status", "COMPLETED");

        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        adapter.publishToKafka("TRANSACTION", null, agentId, details);

        verify(kafkaTemplate).send(eq("efm.events"), anyString(), any());
    }

    @Test
    void publishToKafka_callsEfmEventPublisher() {
        UUID transactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        Map<String, Object> details = Map.of("amount", "1000.00");

        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        adapter.publishToKafka("TRANSACTION", transactionId, agentId, details);

        verify(efmEventPublisher).publishEvent(eq("TRANSACTION"), eq(transactionId), eq(agentId), eq(details));
    }

    @Test
    void publishFraudAlertToKafka_sendsFraudAlert() {
        UUID transactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        adapter.publishFraudAlertToKafka("GEOLOCATION_VIOLATION", transactionId, agentId, "Agent moved 50km");

        verify(kafkaTemplate).send(eq("efm.events"), eq(agentId.toString()), any());
    }

    @Test
    void publishFraudAlertToKafka_callsEfmEventPublisher() {
        UUID transactionId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();

        when(kafkaTemplate.send(anyString(), anyString(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        adapter.publishFraudAlertToKafka("SUSPICIOUS_PATTERN", transactionId, agentId, "High frequency transactions");

        verify(efmEventPublisher).publishFraudAlert(eq("SUSPICIOUS_PATTERN"), eq(transactionId), eq(agentId), eq("High frequency transactions"));
    }
}
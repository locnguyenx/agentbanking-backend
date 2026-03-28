package com.agentbanking.ledger.infrastructure.messaging;

import com.agentbanking.common.efm.EfmEventPublisher;
import com.agentbanking.common.messaging.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class EfmEventPublisherAdapter {

    private static final Logger log = LoggerFactory.getLogger(EfmEventPublisherAdapter.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EfmEventPublisher efmEventPublisher;

    public EfmEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate,
                                     EfmEventPublisher efmEventPublisher) {
        this.kafkaTemplate = kafkaTemplate;
        this.efmEventPublisher = efmEventPublisher;
    }

    public void publishToKafka(String eventType, UUID transactionId, UUID agentId,
                                Map<String, Object> details) {
        // First publish via the component (logs locally)
        efmEventPublisher.publishEvent(eventType, transactionId, agentId, details);

        // Then publish to Kafka for downstream EFM consumers
        try {
            KafkaEfmEvent event = new KafkaEfmEvent(
                    UUID.randomUUID().toString(),
                    eventType,
                    transactionId != null ? transactionId.toString() : null,
                    agentId != null ? agentId.toString() : null,
                    details,
                    java.time.Instant.now().toString()
            );

            String key = agentId != null ? agentId.toString() : UUID.randomUUID().toString();
            kafkaTemplate.send(KafkaTopics.EFM_EVENTS, key, event);

            log.info("Published EFM event to Kafka: type={}, txId={}", eventType, transactionId);
        } catch (Exception e) {
            log.error("Failed to publish EFM event to Kafka: type={}", eventType, e);
        }
    }

    public void publishFraudAlertToKafka(String alertType, UUID transactionId, UUID agentId,
                                          String reason) {
        efmEventPublisher.publishFraudAlert(alertType, transactionId, agentId, reason);

        try {
            KafkaEfmEvent event = new KafkaEfmEvent(
                    UUID.randomUUID().toString(),
                    "FRAUD_ALERT:" + alertType,
                    transactionId != null ? transactionId.toString() : null,
                    agentId != null ? agentId.toString() : null,
                    Map.of("reason", reason),
                    java.time.Instant.now().toString()
            );

            String key = agentId != null ? agentId.toString() : UUID.randomUUID().toString();
            kafkaTemplate.send(KafkaTopics.EFM_EVENTS, key, event);

            log.warn("Published fraud alert to Kafka: type={}, txId={}", alertType, transactionId);
        } catch (Exception e) {
            log.error("Failed to publish fraud alert to Kafka: type={}", alertType, e);
        }
    }

    public record KafkaEfmEvent(
            String eventId,
            String eventType,
            String transactionId,
            String agentId,
            Map<String, Object> details,
            String timestamp
    ) {}
}

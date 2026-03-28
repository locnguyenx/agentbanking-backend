package com.agentbanking.common.efm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event Forward Monitoring (EFM) for publishing transaction events to Kafka.
 * Used for real-time monitoring and fraud detection.
 */
@Component
public class EfmEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EfmEventPublisher.class);

    public void publishEvent(String eventType, UUID transactionId, UUID agentId,
                             Map<String, Object> details) {
        EfmEvent event = new EfmEvent(
            UUID.randomUUID(),
            eventType,
            transactionId,
            agentId,
            details,
            LocalDateTime.now()
        );

        log.info("EFM Event: type={}, txId={}, agentId={}, details={}",
                 eventType, transactionId, agentId, details);
    }

    public void publishFraudAlert(String alertType, UUID transactionId, UUID agentId,
                                  String reason) {
        log.warn("FRAUD ALERT: type={}, txId={}, agentId={}, reason={}",
                 alertType, transactionId, agentId, reason);
    }

    public record EfmEvent(
        UUID eventId,
        String eventType,
        UUID transactionId,
        UUID agentId,
        Map<String, Object> details,
        LocalDateTime timestamp
    ) {}
}

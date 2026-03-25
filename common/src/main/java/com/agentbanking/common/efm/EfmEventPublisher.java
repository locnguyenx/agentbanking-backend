package com.agentbanking.common.efm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event Forward Monitoring (EFM) for publishing transaction events to Kafka.
 * Used for real-time monitoring and fraud detection.
 */
public class EfmEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EfmEventPublisher.class);

    /**
     * Publishes a transaction event for EFM monitoring.
     * In production, this would publish to Kafka.
     * 
     * @param eventType Type of event (TRANSACTION, REVERSAL, KYC, etc.)
     * @param transactionId Internal transaction ID
     * @param agentId Agent ID
     * @param details Event details
     */
    public static void publishEvent(String eventType, UUID transactionId, UUID agentId, 
                                     Map<String, Object> details) {
        EfmEvent event = new EfmEvent(
            UUID.randomUUID(),
            eventType,
            transactionId,
            agentId,
            details,
            LocalDateTime.now()
        );
        
        // In production, publish to Kafka
        // For now, log the event
        log.info("EFM Event: type={}, txId={}, agentId={}, details={}", 
                 eventType, transactionId, agentId, details);
    }

    /**
     * Publishes a fraud alert event.
     * 
     * @param alertType Type of alert (GEOFENCE_VIOLATION, VELOCITY_EXCEEDED, etc.)
     * @param transactionId Transaction ID
     * @param agentId Agent ID
     * @param reason Alert reason
     */
    public static void publishFraudAlert(String alertType, UUID transactionId, UUID agentId, 
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

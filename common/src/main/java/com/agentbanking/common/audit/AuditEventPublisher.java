package com.agentbanking.common.audit;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public interface AuditEventPublisher {
    void publish(AuditEvent event);

    record AuditEvent(
        UUID auditId, String serviceName, String entityType, UUID entityId,
        AuditEventType action, String performedBy, String ipAddress,
        LocalDateTime timestamp, String outcome, String failureReason,
        Map<String, Object> changes
    ) {
        public static AuditEvent success(String serviceName, String entityType, UUID entityId,
                                         AuditEventType action, String performedBy, String ipAddress) {
            return new AuditEvent(UUID.randomUUID(), serviceName, entityType, entityId, action,
                performedBy, ipAddress, LocalDateTime.now(), "SUCCESS", null, Map.of());
        }
        public static AuditEvent failure(String serviceName, String entityType, UUID entityId,
                                         AuditEventType action, String performedBy, String ipAddress, String failureReason) {
            return new AuditEvent(UUID.randomUUID(), serviceName, entityType, entityId, action,
                performedBy, ipAddress, LocalDateTime.now(), "FAILURE", failureReason, Map.of());
        }
    }
}

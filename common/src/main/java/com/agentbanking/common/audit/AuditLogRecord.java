package com.agentbanking.common.audit;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Audit log record - captures all audit events across services.
 * Compliant with Bank Malaysia standards for financial audit trails.
 */
public record AuditLogRecord(
    UUID auditId,
    String entityType,
    UUID entityId,
    AuditAction action,
    String performedBy,
    String changes,
    String ipAddress,
    LocalDateTime timestamp,
    AuditOutcome outcome,
    String failureReason,
    String traceId,
    String sessionId,
    String serviceName,
    String deviceInfo,
    String geographicLocation
) {
    public AuditLogRecord {
        if (auditId == null) throw new NullPointerException("auditId cannot be null");
        if (entityType == null) throw new NullPointerException("entityType cannot be null");
        if (action == null) throw new NullPointerException("action cannot be null");
        if (performedBy == null) throw new NullPointerException("performedBy cannot be null");
        if (outcome == null) throw new NullPointerException("outcome cannot be null");
        if (serviceName == null) throw new NullPointerException("serviceName cannot be null");
    }

    /**
     * Factory method for success events
     */
    public static AuditLogRecord success(String serviceName, String entityType, UUID entityId,
            AuditAction action, String performedBy, String ipAddress, String changes) {
        return new AuditLogRecord(
            UUID.randomUUID(), entityType, entityId, action, performedBy, changes,
            ipAddress != null ? ipAddress : "unknown", LocalDateTime.now(),
            AuditOutcome.SUCCESS, null, null, null, serviceName, null, null
        );
    }

    /**
     * Factory method for failure events
     */
    public static AuditLogRecord failure(String serviceName, String entityType, UUID entityId,
            AuditAction action, String performedBy, String ipAddress, String failureReason) {
        return new AuditLogRecord(
            UUID.randomUUID(), entityType, entityId, action, performedBy, null,
            ipAddress != null ? ipAddress : "unknown", LocalDateTime.now(),
            AuditOutcome.FAILURE, failureReason, null, null, serviceName, null, null
        );
    }
}
package com.agentbanking.auth.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain record for audit logs in the Auth/IAM service
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
    String failureReason
) {}
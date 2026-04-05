package com.agentbanking.audit.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogRecord(
    UUID auditId,
    String serviceName,
    String entityType,
    UUID entityId,
    String action,
    String performedBy,
    String ipAddress,
    LocalDateTime timestamp,
    String outcome,
    String failureReason,
    String changes,
    LocalDateTime createdAt
) {}

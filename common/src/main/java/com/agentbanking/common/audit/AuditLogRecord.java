package com.agentbanking.common.audit;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogRecord(
    UUID auditId,
    String entityType,
    UUID entityId,
    AuditAction action,
    String performedBy,
    String changes,
    String ipAddress,
    LocalDateTime timestamp
) {
    public AuditLogRecord {
        if (auditId == null) throw new NullPointerException("auditId cannot be null");
        if (entityType == null) throw new NullPointerException("entityType cannot be null");
        if (action == null) throw new NullPointerException("action cannot be null");
        if (performedBy == null) throw new NullPointerException("performedBy cannot be null");
    }
}
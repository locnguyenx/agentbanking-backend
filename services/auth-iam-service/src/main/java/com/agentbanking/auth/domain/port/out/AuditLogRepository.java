package com.agentbanking.auth.domain.port.out;

import com.agentbanking.auth.domain.model.AuditLogRecord;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for audit log persistence operations.
 */
public interface AuditLogRepository {
    Optional<AuditLogRecord> findById(UUID auditLogId);
    AuditLogRecord save(AuditLogRecord auditLogRecord);
}
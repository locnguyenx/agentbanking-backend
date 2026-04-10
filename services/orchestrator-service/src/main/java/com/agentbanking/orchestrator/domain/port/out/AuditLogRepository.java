package com.agentbanking.orchestrator.domain.port.out;

import com.agentbanking.common.audit.AuditLogRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLogRepository {
    Optional<AuditLogRecord> findById(UUID auditLogId);
    AuditLogRecord save(AuditLogRecord record);
    List<AuditLogRecord> findAll(int page, int size);
    long count();
}
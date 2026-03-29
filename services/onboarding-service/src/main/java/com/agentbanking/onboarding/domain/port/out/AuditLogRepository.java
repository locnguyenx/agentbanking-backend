package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.common.audit.AuditLogRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogRepository {
    AuditLogRecord save(AuditLogRecord record);
    Page<AuditLogRecord> searchAuditLogs(String entityType, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);
}

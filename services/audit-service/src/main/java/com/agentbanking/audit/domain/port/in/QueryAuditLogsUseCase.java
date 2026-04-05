package com.agentbanking.audit.domain.port.in;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import java.time.LocalDateTime;
import java.util.List;

public interface QueryAuditLogsUseCase {
    AuditLogPage queryAuditLogs(
        String serviceName,
        String action,
        String performedBy,
        String outcome,
        LocalDateTime from,
        LocalDateTime to,
        int page,
        int size
    );

    record AuditLogPage(
        List<AuditLogRecord> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) {}
}

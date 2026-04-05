package com.agentbanking.audit.domain.port.out;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import com.agentbanking.audit.domain.port.in.QueryAuditLogsUseCase.AuditLogPage;
import java.time.LocalDateTime;

public interface AuditLogRepository {
    AuditLogRecord save(AuditLogRecord record);
    AuditLogPage findByFilters(
        String serviceName,
        String action,
        String performedBy,
        String outcome,
        LocalDateTime from,
        LocalDateTime to,
        int page,
        int size
    );
}

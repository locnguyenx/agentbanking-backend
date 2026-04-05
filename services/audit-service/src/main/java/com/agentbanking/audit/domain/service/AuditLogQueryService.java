package com.agentbanking.audit.domain.service;

import com.agentbanking.audit.domain.model.AuditLogRecord;
import com.agentbanking.audit.domain.port.in.QueryAuditLogsUseCase;
import com.agentbanking.audit.domain.port.out.AuditLogRepository;
import java.time.LocalDateTime;

public class AuditLogQueryService implements QueryAuditLogsUseCase {

    private final AuditLogRepository auditLogRepository;

    public AuditLogQueryService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLogPage queryAuditLogs(
        String serviceName, String action, String performedBy,
        String outcome, LocalDateTime from, LocalDateTime to,
        int page, int size
    ) {
        return auditLogRepository.findByFilters(
            serviceName, action, performedBy, outcome, from, to, page, size
        );
    }
}

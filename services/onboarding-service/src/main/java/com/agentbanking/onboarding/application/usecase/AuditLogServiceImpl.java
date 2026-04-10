package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.common.audit.AuditLogService;
import com.agentbanking.onboarding.domain.port.out.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public AuditLogRecord log(AuditLogRecord record) {
        if (record.auditId() == null) {
            record = new AuditLogRecord(
                record.auditId() != null ? record.auditId() : UUID.randomUUID(),
                record.entityType(),
                record.entityId(),
                record.action(),
                record.performedBy(),
                record.changes(),
                record.ipAddress() != null ? record.ipAddress() : "unknown",
                record.timestamp() != null ? record.timestamp() : LocalDateTime.now(),
                record.outcome(),
                record.failureReason(),
                record.traceId(),
                record.sessionId(),
                record.serviceName() != null ? record.serviceName() : "onboarding-service",
                record.deviceInfo(),
                record.geographicLocation()
            );
        }
        return auditLogRepository.save(record);
    }
}

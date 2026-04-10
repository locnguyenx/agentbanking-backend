package com.agentbanking.biller.application.usecase;

import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.biller.domain.port.out.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogServiceImpl {
    
    private final AuditLogRepository auditLogRepository;
    
    public AuditLogServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    public AuditLogRecord log(AuditLogRecord record) {
        if (record.auditId() == null) {
            record = new AuditLogRecord(
                record.auditId(),
                record.entityType(),
                record.entityId(),
                record.action(),
                record.performedBy(),
                record.changes(),
                record.ipAddress() != null ? record.ipAddress() : "unknown",
                record.timestamp(),
                record.outcome(),
                record.failureReason(),
                record.traceId(),
                record.sessionId(),
                record.serviceName() != null ? record.serviceName() : "biller-service",
                record.deviceInfo(),
                record.geographicLocation()
            );
        }
        return auditLogRepository.save(record);
    }
    
    public List<AuditLogRecord> getAuditLogs(int page, int size) {
        return auditLogRepository.findAll(page, size);
    }
    
    public long getTotalCount() {
        return auditLogRepository.count();
    }
}
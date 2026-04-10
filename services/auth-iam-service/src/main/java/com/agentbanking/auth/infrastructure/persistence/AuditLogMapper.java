package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.common.audit.AuditAction;
import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.common.audit.AuditOutcome;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogRecord toRecord(AuditLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AuditLogRecord(
            entity.getAuditId(),
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getAction(),
            entity.getPerformedBy(),
            entity.getChanges(),
            entity.getIpAddress(),
            entity.getTimestamp(),
            entity.getOutcome(),
            entity.getFailureReason(),
            entity.getTraceId(),
            entity.getSessionId(),
            entity.getServiceName(),
            entity.getDeviceInfo(),
            entity.getGeographicLocation()
        );
    }

    public AuditLogEntity toEntity(AuditLogRecord record) {
        if (record == null) {
            return null;
        }
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAuditId(record.auditId());
        entity.setEntityType(record.entityType());
        entity.setEntityId(record.entityId());
        entity.setAction(record.action());
        entity.setPerformedBy(record.performedBy());
        entity.setChanges(record.changes());
        entity.setIpAddress(record.ipAddress() != null ? record.ipAddress() : "unknown");
        entity.setTimestamp(record.timestamp());
        entity.setOutcome(record.outcome());
        entity.setFailureReason(record.failureReason());
        entity.setTraceId(record.traceId());
        entity.setSessionId(record.sessionId());
        entity.setServiceName(record.serviceName());
        entity.setDeviceInfo(record.deviceInfo());
        entity.setGeographicLocation(record.geographicLocation());
        return entity;
    }
}
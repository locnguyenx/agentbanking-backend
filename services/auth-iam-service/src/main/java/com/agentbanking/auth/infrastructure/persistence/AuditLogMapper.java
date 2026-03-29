package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.AuditLogRecord;
import com.agentbanking.auth.domain.model.AuditAction;
import com.agentbanking.auth.domain.model.AuditOutcome;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper to convert between AuditLogEntity and AuditLogRecord
 */
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
                entity.getFailureReason()
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
        entity.setIpAddress(record.ipAddress());
        entity.setTimestamp(record.timestamp());
        entity.setOutcome(record.outcome());
        entity.setFailureReason(record.failureReason());
        return entity;
    }
}
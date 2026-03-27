package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.onboarding.domain.port.out.AuditLogRepository;
import com.agentbanking.onboarding.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;

    public AuditLogRepositoryImpl(AuditLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuditLogRecord save(AuditLogRecord record) {
        AuditLogEntity entity = toEntity(record);
        AuditLogEntity saved = jpaRepository.save(entity);
        return toRecord(saved);
    }

    private AuditLogEntity toEntity(AuditLogRecord record) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAuditId(record.auditId());
        entity.setEntityType(record.entityType());
        entity.setEntityId(record.entityId());
        entity.setAction(record.action());
        entity.setPerformedBy(record.performedBy());
        entity.setChanges(record.changes());
        entity.setIpAddress(record.ipAddress());
        entity.setTimestamp(record.timestamp() != null ? record.timestamp() : LocalDateTime.now());
        return entity;
    }

    private AuditLogRecord toRecord(AuditLogEntity entity) {
        return new AuditLogRecord(
            entity.getAuditId(),
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getAction(),
            entity.getPerformedBy(),
            entity.getChanges(),
            entity.getIpAddress(),
            entity.getTimestamp()
        );
    }
}

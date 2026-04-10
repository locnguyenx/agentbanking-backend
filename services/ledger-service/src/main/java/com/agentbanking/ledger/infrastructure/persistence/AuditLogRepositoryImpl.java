package com.agentbanking.ledger.infrastructure.persistence;

import com.agentbanking.common.audit.AuditAction;
import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.common.audit.AuditOutcome;
import com.agentbanking.ledger.domain.port.out.AuditLogRepository;
import com.agentbanking.ledger.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {
    
    private static final String SERVICE_NAME = "ledger-service";
    
    private final AuditLogJpaRepository auditLogJpaRepository;
    
    public AuditLogRepositoryImpl(AuditLogJpaRepository auditLogJpaRepository) {
        this.auditLogJpaRepository = auditLogJpaRepository;
    }
    
    @Override
    public Optional<AuditLogRecord> findById(UUID auditLogId) {
        return auditLogJpaRepository.findById(auditLogId).map(this::toRecord);
    }
    
    @Override
    public AuditLogRecord save(AuditLogRecord record) {
        AuditLogEntity entity = toEntity(record);
        if (entity.getTimestamp() == null) {
            entity.setTimestamp(java.time.LocalDateTime.now());
        }
        if (entity.getIpAddress() == null) {
            entity.setIpAddress("unknown");
        }
        if (entity.getServiceName() == null) {
            entity.setServiceName(SERVICE_NAME);
        }
        AuditLogEntity saved = auditLogJpaRepository.save(entity);
        return toRecord(saved);
    }
    
    @Override
    public List<AuditLogRecord> findAll(int page, int size) {
        Page<AuditLogEntity> entities = auditLogJpaRepository.findAll(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        return entities.getContent().stream().map(this::toRecord).toList();
    }
    
    @Override
    public long count() {
        return auditLogJpaRepository.count();
    }
    
    private AuditLogRecord toRecord(AuditLogEntity entity) {
        if (entity == null) return null;
        AuditAction action = entity.getAction() != null ? AuditAction.valueOf(entity.getAction()) : null;
        AuditOutcome outcome = entity.getOutcome() != null ? AuditOutcome.valueOf(entity.getOutcome()) : null;
        return new AuditLogRecord(
            entity.getAuditId(), entity.getEntityType(), entity.getEntityId(),
            action, entity.getPerformedBy(), entity.getChanges(),
            entity.getIpAddress(), entity.getTimestamp(), outcome,
            entity.getFailureReason(), entity.getTraceId(), entity.getSessionId(),
            entity.getServiceName(), entity.getDeviceInfo(), entity.getGeographicLocation()
        );
    }
    
    private AuditLogEntity toEntity(AuditLogRecord record) {
        if (record == null) return null;
        AuditLogEntity entity = new AuditLogEntity();
        entity.setAuditId(record.auditId());
        entity.setEntityType(record.entityType());
        entity.setEntityId(record.entityId());
        entity.setAction(record.action() != null ? record.action().name() : null);
        entity.setPerformedBy(record.performedBy());
        entity.setChanges(record.changes());
        entity.setIpAddress(record.ipAddress() != null ? record.ipAddress() : "unknown");
        entity.setTimestamp(record.timestamp());
        entity.setOutcome(record.outcome() != null ? record.outcome().name() : null);
        entity.setFailureReason(record.failureReason());
        entity.setTraceId(record.traceId());
        entity.setSessionId(record.sessionId());
        entity.setServiceName(record.serviceName() != null ? record.serviceName() : SERVICE_NAME);
        entity.setDeviceInfo(record.deviceInfo());
        entity.setGeographicLocation(record.geographicLocation());
        return entity;
    }
}
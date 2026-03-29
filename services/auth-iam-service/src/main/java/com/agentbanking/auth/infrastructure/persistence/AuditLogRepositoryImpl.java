package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.AuditLogRecord;
import com.agentbanking.auth.domain.port.out.AuditLogRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of AuditLogRepository port
 */
@Repository
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final AuditLogJpaRepository auditLogJpaRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogRepositoryImpl(AuditLogJpaRepository auditLogJpaRepository, AuditLogMapper auditLogMapper) {
        this.auditLogJpaRepository = auditLogJpaRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public Optional<AuditLogRecord> findById(UUID auditLogId) {
        return auditLogJpaRepository.findById(auditLogId)
                .map(auditLogMapper::toRecord);
    }

    @Override
    public AuditLogRecord save(AuditLogRecord auditLogRecord) {
        AuditLogEntity entity = auditLogMapper.toEntity(auditLogRecord);
        AuditLogEntity saved = auditLogJpaRepository.save(entity);
        return auditLogMapper.toRecord(saved);
    }
}
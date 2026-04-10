package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.auth.domain.port.out.AuditLogRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public List<AuditLogRecord> findAll(int page, int size) {
        List<AuditLogEntity> entities = auditLogJpaRepository.findAll(
            org.springframework.data.domain.PageRequest.of(page, size, 
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "timestamp"))
        ).getContent();
        return entities.stream().map(auditLogMapper::toRecord).toList();
    }

    @Override
    public long count() {
        return auditLogJpaRepository.count();
    }
}
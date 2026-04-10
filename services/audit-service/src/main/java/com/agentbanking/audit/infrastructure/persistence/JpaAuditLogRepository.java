package com.agentbanking.audit.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface JpaAuditLogRepository extends JpaRepository<JpaAuditLogEntity, UUID> {

    Page<JpaAuditLogEntity> findByServiceName(String serviceName, Pageable pageable);
    
    Page<JpaAuditLogEntity> findByAction(String action, Pageable pageable);
    
    Page<JpaAuditLogEntity> findByPerformedBy(String performedBy, Pageable pageable);
    
    Page<JpaAuditLogEntity> findByOutcome(String outcome, Pageable pageable);
    
    Page<JpaAuditLogEntity> findByTimestampBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
    
    Page<JpaAuditLogEntity> findAll(Pageable pageable);
}

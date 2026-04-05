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

    @Query("SELECT e FROM JpaAuditLogEntity e WHERE " +
           "(:serviceName IS NULL OR e.serviceName = :serviceName) AND " +
           "(:action IS NULL OR e.action = :action) AND " +
           "(:performedBy IS NULL OR e.performedBy = :performedBy) AND " +
           "(:outcome IS NULL OR e.outcome = :outcome) AND " +
           "(:from IS NULL OR e.timestamp >= :from) AND " +
           "(:to IS NULL OR e.timestamp <= :to)")
    Page<JpaAuditLogEntity> findByFilters(
        @Param("serviceName") String serviceName,
        @Param("action") String action,
        @Param("performedBy") String performedBy,
        @Param("outcome") String outcome,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );
}

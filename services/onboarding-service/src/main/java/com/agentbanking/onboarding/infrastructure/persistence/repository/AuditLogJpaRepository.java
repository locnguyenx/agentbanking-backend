package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {

    @Modifying
    @Query("DELETE FROM AuditLogEntity a WHERE a.auditId = ?1")
    void deleteByAuditId(UUID auditId);

    @Modifying
    @Query("UPDATE AuditLogEntity a SET a.entityType = ?1 WHERE a.auditId = ?2")
    void updateEntityType(String entityType, UUID auditId);

    // Query methods for audit log viewer
    Page<AuditLogEntity> findByEntityType(String entityType, Pageable pageable);
    
    Page<AuditLogEntity> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);
    
    @Query("SELECT a FROM AuditLogEntity a WHERE " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:fromDate IS NULL OR a.timestamp >= :fromDate) AND " +
           "(:toDate IS NULL OR a.timestamp <= :toDate) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLogEntity> searchAuditLogs(
            @Param("entityType") String entityType,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
    
    // Simple query for when all parameters are null
    @Query("SELECT a FROM AuditLogEntity a ORDER BY a.timestamp DESC")
    Page<AuditLogEntity> findAllOrderByTimestampDesc(Pageable pageable);
}

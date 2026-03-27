package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID> {

    @Modifying
    @Query("DELETE FROM AuditLogEntity a WHERE a.auditId = ?1")
    void deleteByAuditId(UUID auditId);

    @Modifying
    @Query("UPDATE AuditLogEntity a SET a.entityType = ?1 WHERE a.auditId = ?2")
    void updateEntityType(String entityType, UUID auditId);
}

package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.infrastructure.persistence.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for PermissionEntity
 */
@Repository
public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, UUID> {
    Optional<PermissionEntity> findByPermissionKey(String permissionKey);
}
package com.agentbanking.auth.domain.port.out;

import com.agentbanking.auth.domain.model.PermissionRecord;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for permission persistence operations.
 */
public interface PermissionRepository {
    Optional<PermissionRecord> findById(UUID permissionId);
    Optional<PermissionRecord> findByKey(String permissionKey);
    PermissionRecord save(PermissionRecord permissionRecord);
    boolean deleteById(UUID permissionId);
}
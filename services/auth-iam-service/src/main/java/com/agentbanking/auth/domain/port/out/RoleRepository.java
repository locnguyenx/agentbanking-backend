package com.agentbanking.auth.domain.port.out;

import com.agentbanking.auth.domain.model.RoleRecord;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for role persistence operations.
 */
public interface RoleRepository {
    Optional<RoleRecord> findById(UUID roleId);
    Optional<RoleRecord> findByName(String roleName);
    RoleRecord save(RoleRecord roleRecord);
    boolean deleteById(UUID roleId);
}
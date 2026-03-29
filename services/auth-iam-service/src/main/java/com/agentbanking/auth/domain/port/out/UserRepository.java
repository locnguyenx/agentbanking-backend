package com.agentbanking.auth.domain.port.out;

import com.agentbanking.auth.domain.model.UserRecord;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for user persistence operations.
 */
public interface UserRepository {
    Optional<UserRecord> findById(UUID userId);
    Optional<UserRecord> findByUsername(String username);
    Optional<UserRecord> findByEmail(String email);
    UserRecord save(UserRecord userRecord);
    boolean deleteById(UUID userId);
}
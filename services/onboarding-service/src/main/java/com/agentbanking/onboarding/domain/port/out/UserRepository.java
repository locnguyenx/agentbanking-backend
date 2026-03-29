package com.agentbanking.onboarding.domain.port.out;

import com.agentbanking.onboarding.domain.model.UserRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for user repository
 */
public interface UserRepository {
    UserRecord save(UserRecord user);
    Optional<UserRecord> findById(UUID userId);
    Optional<UserRecord> findByUsername(String username);
    Page<UserRecord> findAll(Pageable pageable);
    void deleteById(UUID userId);
    boolean existsByUsername(String username);
}

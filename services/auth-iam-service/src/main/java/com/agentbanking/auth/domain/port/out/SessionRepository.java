package com.agentbanking.auth.domain.port.out;

import com.agentbanking.auth.domain.model.SessionRecord;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for session persistence operations.
 */
public interface SessionRepository {
    Optional<SessionRecord> findById(UUID sessionId);
    Optional<SessionRecord> findByToken(String token);
    SessionRecord save(SessionRecord sessionRecord);
    boolean deleteById(UUID sessionId);
}
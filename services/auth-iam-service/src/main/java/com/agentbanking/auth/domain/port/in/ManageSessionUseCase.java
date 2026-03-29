package com.agentbanking.auth.domain.port.in;

import com.agentbanking.auth.domain.model.SessionRecord;

import java.util.UUID;

/**
 * Inbound port for session management use cases.
 */
public interface ManageSessionUseCase {
    SessionRecord createSession(SessionRecord sessionRecord);
    SessionRecord getSessionById(UUID sessionId);
    SessionRecord getSessionByUserId(UUID userId);
    boolean updateSession(UUID sessionId, SessionRecord sessionRecord);
    boolean revokeSession(UUID sessionId);
    boolean revokeAllSessionsForUser(UUID userId);
    boolean isValidSession(UUID sessionId);
    void updateLastAccessed(UUID sessionId);
}
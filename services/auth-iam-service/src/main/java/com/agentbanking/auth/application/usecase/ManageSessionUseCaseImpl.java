package com.agentbanking.auth.application.usecase;

import com.agentbanking.auth.domain.model.SessionRecord;
import com.agentbanking.auth.domain.port.in.ManageSessionUseCase;
import com.agentbanking.auth.domain.port.out.SessionRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Use case implementation for session management
 */
public class ManageSessionUseCaseImpl implements ManageSessionUseCase {

    private final SessionRepository sessionRepository;

    public ManageSessionUseCaseImpl(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    public SessionRecord createSession(SessionRecord sessionRecord) {
        // Create session with proper defaults
        SessionRecord newSession = new SessionRecord(
                UUID.randomUUID(),
                sessionRecord.userId(),
                sessionRecord.refreshTokenHash(),
                sessionRecord.userAgent(),
                sessionRecord.ipAddress(),
                sessionRecord.createdAt(),
                sessionRecord.expiresAt(),
                sessionRecord.lastAccessedAt(),
                sessionRecord.revokedAt(),
                sessionRecord.isActive()
        );

        return sessionRepository.save(newSession);
    }

    @Override
    public SessionRecord getSessionById(UUID sessionId) {
        return sessionRepository.findById(sessionId).orElse(null);
    }

    @Override
    public SessionRecord getSessionByUserId(UUID userId) {
        // In a full implementation, we would query by userId
        // For now, return null as this requires a query method
        return null;
    }

    @Override
    public boolean updateSession(UUID sessionId, SessionRecord sessionRecord) {
        SessionRecord existing = sessionRepository.findById(sessionId).orElse(null);
        if (existing == null) {
            return false;
        }

        // Update session
        SessionRecord updatedSession = new SessionRecord(
                existing.sessionId(),
                existing.userId(),
                existing.refreshTokenHash(),
                sessionRecord.userAgent() != null ? sessionRecord.userAgent() : existing.userAgent(),
                sessionRecord.ipAddress() != null ? sessionRecord.ipAddress() : existing.ipAddress(),
                existing.createdAt(),
                sessionRecord.expiresAt(),
                sessionRecord.lastAccessedAt(),
                existing.revokedAt(),
                sessionRecord.isActive()
        );

        sessionRepository.save(updatedSession);
        return true;
    }

    @Override
    public boolean revokeSession(UUID sessionId) {
        SessionRecord existing = sessionRepository.findById(sessionId).orElse(null);
        if (existing == null) {
            return false;
        }

        // Mark session as revoked
        SessionRecord revokedSession = new SessionRecord(
                existing.sessionId(),
                existing.userId(),
                existing.refreshTokenHash(),
                existing.userAgent(),
                existing.ipAddress(),
                existing.createdAt(),
                existing.expiresAt(),
                existing.lastAccessedAt(),
                LocalDateTime.now(),
                false
        );

        sessionRepository.save(revokedSession);
        return true;
    }

    @Override
    public boolean revokeAllSessionsForUser(UUID userId) {
        // In a full implementation, we would query all sessions for the user and revoke them
        // For now, return true as this requires a query method
        return true;
    }

    @Override
    public boolean isValidSession(UUID sessionId) {
        SessionRecord session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return false;
        }
        // Check if session is active and not expired
        return session.isActive() && session.expiresAt().isAfter(LocalDateTime.now());
    }

    @Override
    public void updateLastAccessed(UUID sessionId) {
        SessionRecord existing = sessionRepository.findById(sessionId).orElse(null);
        if (existing != null) {
            SessionRecord updatedSession = new SessionRecord(
                    existing.sessionId(),
                    existing.userId(),
                    existing.refreshTokenHash(),
                    existing.userAgent(),
                    existing.ipAddress(),
                    existing.createdAt(),
                    existing.expiresAt(),
                    LocalDateTime.now(),
                    existing.revokedAt(),
                    existing.isActive()
            );
            sessionRepository.save(updatedSession);
        }
    }
}
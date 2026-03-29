package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.SessionRecord;
import com.agentbanking.auth.domain.port.out.SessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of SessionRepository port
 */
@Repository
public class SessionRepositoryImpl implements SessionRepository {

    private final SessionJpaRepository sessionJpaRepository;
    private final SessionMapper sessionMapper;

    public SessionRepositoryImpl(SessionJpaRepository sessionJpaRepository, SessionMapper sessionMapper) {
        this.sessionJpaRepository = sessionJpaRepository;
        this.sessionMapper = sessionMapper;
    }

    @Override
    public Optional<SessionRecord> findById(UUID sessionId) {
        return sessionJpaRepository.findById(sessionId)
                .map(sessionMapper::toRecord);
    }

    @Override
    public Optional<SessionRecord> findByToken(String token) {
        return sessionJpaRepository.findByToken(token)
                .map(sessionMapper::toRecord);
    }

    @Override
    public SessionRecord save(SessionRecord sessionRecord) {
        SessionEntity entity = sessionMapper.toEntity(sessionRecord);
        SessionEntity saved = sessionJpaRepository.save(entity);
        return sessionMapper.toRecord(saved);
    }

    @Override
    public boolean deleteById(UUID sessionId) {
        if (sessionJpaRepository.existsById(sessionId)) {
            sessionJpaRepository.deleteById(sessionId);
            return true;
        }
        return false;
    }
}
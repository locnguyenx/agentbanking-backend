package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.SessionRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper to convert between SessionEntity and SessionRecord
 */
@Component
public class SessionMapper {

    public SessionRecord toRecord(SessionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SessionRecord(
                entity.getSessionId(),
                entity.getUserId(),
                entity.getRefreshTokenHash(),
                entity.getUserAgent(),
                entity.getIpAddress(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getLastAccessedAt(),
                entity.getRevokedAt(),
                entity.getIsActive()
        );
    }

    public SessionEntity toEntity(SessionRecord record) {
        if (record == null) {
            return null;
        }
        SessionEntity entity = new SessionEntity();
        entity.setSessionId(record.sessionId());
        entity.setUserId(record.userId());
        entity.setRefreshTokenHash(record.refreshTokenHash());
        entity.setUserAgent(record.userAgent());
        entity.setIpAddress(record.ipAddress());
        entity.setExpiresAt(record.expiresAt());
        entity.setCreatedAt(record.createdAt());
        entity.setLastAccessedAt(record.lastAccessedAt());
        entity.setRevokedAt(record.revokedAt());
        entity.setIsActive(record.isActive());
        return entity;
    }
}
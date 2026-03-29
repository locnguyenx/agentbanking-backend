package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.TokenBlacklistRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper to convert between TokenBlacklistEntity and TokenBlacklistRecord
 */
@Component
public class TokenBlacklistMapper {

    public TokenBlacklistRecord toRecord(TokenBlacklistEntity entity) {
        if (entity == null) {
            return null;
        }
        return new TokenBlacklistRecord(
                entity.getBlacklistId(),
                entity.getTokenJti(),
                entity.getUserId(),
                entity.getClientId(),
                entity.getRevokedAt(),
                entity.getExpiresAt(),
                entity.getRevokedBy(),
                entity.getReason()
        );
    }

    public TokenBlacklistEntity toEntity(TokenBlacklistRecord record) {
        if (record == null) {
            return null;
        }
        TokenBlacklistEntity entity = new TokenBlacklistEntity();
        entity.setBlacklistId(record.blacklistId());
        entity.setTokenJti(record.tokenJti());
        entity.setUserId(record.userId());
        entity.setClientId(record.clientId());
        entity.setRevokedAt(record.revokedAt());
        entity.setExpiresAt(record.expiresAt());
        entity.setRevokedBy(record.revokedBy());
        entity.setReason(record.reason());
        return entity;
    }
}
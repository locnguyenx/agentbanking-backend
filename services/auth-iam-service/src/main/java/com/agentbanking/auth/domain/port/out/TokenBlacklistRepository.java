package com.agentbanking.auth.domain.port.out;

import com.agentbanking.auth.domain.model.TokenBlacklistRecord;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for token blacklist persistence operations.
 */
public interface TokenBlacklistRepository {
    Optional<TokenBlacklistRecord> findById(UUID blacklistId);
    Optional<TokenBlacklistRecord> findByTokenId(String tokenId);
    TokenBlacklistRecord save(TokenBlacklistRecord tokenBlacklistRecord);
    boolean deleteById(UUID blacklistId);
}
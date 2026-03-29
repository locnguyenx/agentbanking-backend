package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.TokenBlacklistRecord;
import com.agentbanking.auth.domain.port.out.TokenBlacklistRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of TokenBlacklistRepository port
 */
@Repository
public class TokenBlacklistRepositoryImpl implements TokenBlacklistRepository {

    private final TokenBlacklistJpaRepository tokenBlacklistJpaRepository;
    private final TokenBlacklistMapper tokenBlacklistMapper;

    public TokenBlacklistRepositoryImpl(TokenBlacklistJpaRepository tokenBlacklistJpaRepository, TokenBlacklistMapper tokenBlacklistMapper) {
        this.tokenBlacklistJpaRepository = tokenBlacklistJpaRepository;
        this.tokenBlacklistMapper = tokenBlacklistMapper;
    }

    @Override
    public Optional<TokenBlacklistRecord> findById(UUID blacklistId) {
        return tokenBlacklistJpaRepository.findById(blacklistId)
                .map(tokenBlacklistMapper::toRecord);
    }

    @Override
    public Optional<TokenBlacklistRecord> findByTokenId(String tokenId) {
        return tokenBlacklistJpaRepository.findByTokenJti(tokenId)
                .map(tokenBlacklistMapper::toRecord);
    }

    @Override
    public TokenBlacklistRecord save(TokenBlacklistRecord tokenBlacklistRecord) {
        TokenBlacklistEntity entity = tokenBlacklistMapper.toEntity(tokenBlacklistRecord);
        TokenBlacklistEntity saved = tokenBlacklistJpaRepository.save(entity);
        return tokenBlacklistMapper.toRecord(saved);
    }

    @Override
    public boolean deleteById(UUID blacklistId) {
        if (tokenBlacklistJpaRepository.existsById(blacklistId)) {
            tokenBlacklistJpaRepository.deleteById(blacklistId);
            return true;
        }
        return false;
    }
}
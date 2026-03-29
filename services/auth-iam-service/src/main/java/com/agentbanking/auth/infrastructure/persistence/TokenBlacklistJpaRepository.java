package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.infrastructure.persistence.TokenBlacklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for TokenBlacklistEntity
 */
@Repository
public interface TokenBlacklistJpaRepository extends JpaRepository<TokenBlacklistEntity, UUID> {
    Optional<TokenBlacklistEntity> findByTokenJti(String tokenJti);
}
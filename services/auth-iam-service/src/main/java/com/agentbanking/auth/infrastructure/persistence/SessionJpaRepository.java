package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.infrastructure.persistence.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for SessionEntity
 */
@Repository
public interface SessionJpaRepository extends JpaRepository<SessionEntity, UUID> {
    Optional<SessionEntity> findByToken(String token);
    Optional<SessionEntity> findByTokenId(String tokenId);
}
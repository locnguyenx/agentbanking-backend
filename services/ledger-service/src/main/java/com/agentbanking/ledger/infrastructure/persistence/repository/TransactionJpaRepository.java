package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {
    
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
}

package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {
    
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
    
    @Query("SELECT COUNT(t) FROM TransactionEntity t")
    long countAllTransactions();
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t WHERE t.status = 'SUCCESS'")
    BigDecimal sumSuccessfulTransactionAmount();
    
    @Query("SELECT COUNT(DISTINCT t.agentId) FROM TransactionEntity t WHERE t.status = 'SUCCESS'")
    long countDistinctAgents();
    
    List<TransactionEntity> findByAgentIdOrderByCreatedAtDesc(UUID agentId);
    
    @Query("SELECT t FROM TransactionEntity t ORDER BY t.createdAt DESC")
    List<TransactionEntity> findRecentTransactions();
}

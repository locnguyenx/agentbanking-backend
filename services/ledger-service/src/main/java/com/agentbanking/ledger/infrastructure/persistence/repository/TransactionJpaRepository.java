package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.model.TransactionStatus;
import com.agentbanking.ledger.domain.model.TransactionType;
import com.agentbanking.ledger.infrastructure.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {
    
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);
    
    @Query("SELECT COUNT(t) FROM TransactionEntity t")
    long countAllTransactions();
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t WHERE t.status = 'COMPLETED'")
    BigDecimal sumSuccessfulTransactionAmount();
    
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t WHERE t.status = com.agentbanking.ledger.domain.model.TransactionStatus.COMPLETED AND t.transactionType = :type")
    BigDecimal sumSuccessfulTransactionAmountByType(@Param("type") TransactionType type);
    
    @Query("SELECT COUNT(DISTINCT t.agentId) FROM TransactionEntity t WHERE t.status = 'COMPLETED'")
    long countDistinctAgents();
    
    List<TransactionEntity> findByAgentIdOrderByCreatedAtDesc(UUID agentId);
    
    @Query("SELECT t FROM TransactionEntity t ORDER BY t.createdAt DESC")
    List<TransactionEntity> findRecentTransactions();

    List<TransactionEntity> findByAgentIdAndStatus(UUID agentId, TransactionStatus status);

    long countByAgentIdAndStatus(UUID agentId, TransactionStatus status);

    boolean existsByAgentIdAndStatusIn(UUID agentId, List<TransactionStatus> statuses);

    @Query("SELECT DISTINCT t.agentId FROM TransactionEntity t WHERE t.completedAt BETWEEN :start AND :end")
    List<UUID> findDistinctAgentIdsByCompletedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<TransactionEntity> findByAgentIdAndCompletedAtBetween(UUID agentId, LocalDateTime start, LocalDateTime end);
    
    long countByAgentIdAndStatusAndCompletedAtBetween(UUID agentId, TransactionStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM TransactionEntity t WHERE t.agentId = :agentId AND t.status = :status AND t.completedAt BETWEEN :start AND :end")
    BigDecimal sumAmountByAgentIdAndStatusAndCompletedAtBetween(
        @Param("agentId") UUID agentId, 
        @Param("status") TransactionStatus status, 
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end
    );

    List<TransactionEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}

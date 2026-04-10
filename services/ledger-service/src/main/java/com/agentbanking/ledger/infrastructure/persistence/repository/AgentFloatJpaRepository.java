package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.infrastructure.persistence.entity.AgentFloatEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentFloatJpaRepository extends JpaRepository<AgentFloatEntity, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AgentFloatEntity a WHERE a.agentId = :agentId")
    Optional<AgentFloatEntity> findByAgentIdWithLock(@Param("agentId") UUID agentId);

    @Query("SELECT a FROM AgentFloatEntity a WHERE a.agentId = :agentId")
    Optional<AgentFloatEntity> findByAgentId(@Param("agentId") UUID agentId);
    @Modifying
    @Query("UPDATE AgentFloatEntity a SET a.balance = a.balance + :balanceChange WHERE a.agentId = :agentId")
    void updateBalance(UUID agentId, BigDecimal balanceChange);
}

package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.rules.infrastructure.persistence.entity.FeeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface FeeConfigJpaRepository extends JpaRepository<FeeConfigEntity, UUID> {
    
    @Query("SELECT f FROM FeeConfigEntity f WHERE f.transactionType = :transactionType " +
           "AND f.agentTier = :agentTier " +
           "AND f.effectiveFrom <= :asOfDate " +
           "AND (f.effectiveTo IS NULL OR f.effectiveTo >= :asOfDate)")
    FeeConfigEntity findByTransactionTypeAndAgentTierAndEffectiveDate(
        @Param("transactionType") com.agentbanking.rules.domain.model.TransactionType transactionType,
        @Param("agentTier") com.agentbanking.rules.domain.model.AgentTier agentTier,
        @Param("asOfDate") LocalDate asOfDate);
}

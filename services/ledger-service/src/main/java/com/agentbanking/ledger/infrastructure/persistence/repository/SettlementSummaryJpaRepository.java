package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.infrastructure.persistence.entity.SettlementSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementSummaryJpaRepository extends JpaRepository<SettlementSummaryEntity, UUID> {

    List<SettlementSummaryEntity> findBySettlementDate(LocalDate date);

    Optional<SettlementSummaryEntity> findByAgentIdAndSettlementDate(UUID agentId, LocalDate date);
}

package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.model.DiscrepancyStatus;
import com.agentbanking.ledger.infrastructure.persistence.entity.DiscrepancyCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiscrepancyCaseJpaRepository extends JpaRepository<DiscrepancyCaseEntity, UUID> {
    List<DiscrepancyCaseEntity> findByStatus(DiscrepancyStatus status);
}

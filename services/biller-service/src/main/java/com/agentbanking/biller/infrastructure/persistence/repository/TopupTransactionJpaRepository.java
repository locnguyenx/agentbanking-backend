package com.agentbanking.biller.infrastructure.persistence.repository;

import com.agentbanking.biller.infrastructure.persistence.entity.TopupTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TopupTransactionJpaRepository extends JpaRepository<TopupTransactionEntity, UUID> {
}
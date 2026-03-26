package com.agentbanking.biller.infrastructure.persistence.repository;

import com.agentbanking.biller.infrastructure.persistence.entity.BillPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BillPaymentJpaRepository extends JpaRepository<BillPaymentEntity, UUID> {
}
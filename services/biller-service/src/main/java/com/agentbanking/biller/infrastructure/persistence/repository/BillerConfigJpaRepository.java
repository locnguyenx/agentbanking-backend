package com.agentbanking.biller.infrastructure.persistence.repository;

import com.agentbanking.biller.infrastructure.persistence.entity.BillerConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillerConfigJpaRepository extends JpaRepository<BillerConfigEntity, UUID> {
    Optional<BillerConfigEntity> findByBillerCodeAndActiveTrue(String billerCode);
}
package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.domain.model.KycStatus;
import com.agentbanking.onboarding.infrastructure.persistence.entity.KycVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KycVerificationJpaRepository extends JpaRepository<KycVerificationEntity, UUID> {
    List<KycVerificationEntity> findByVerificationStatusOrderByCreatedAtDesc(KycStatus status);
}
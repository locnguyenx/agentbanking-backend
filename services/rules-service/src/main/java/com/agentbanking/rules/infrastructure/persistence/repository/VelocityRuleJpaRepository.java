package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.rules.infrastructure.persistence.entity.VelocityRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VelocityRuleJpaRepository extends JpaRepository<VelocityRuleEntity, UUID> {
    List<VelocityRuleEntity> findByActiveTrue();
}

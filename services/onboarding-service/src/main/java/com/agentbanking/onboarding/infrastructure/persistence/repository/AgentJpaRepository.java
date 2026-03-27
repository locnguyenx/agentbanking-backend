package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.domain.model.AgentStatus;
import com.agentbanking.onboarding.infrastructure.persistence.entity.AgentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentJpaRepository extends JpaRepository<AgentEntity, UUID> {
    Optional<AgentEntity> findByMykadNumber(String mykadNumber);
    Optional<AgentEntity> findByAgentCode(String agentCode);
    Page<AgentEntity> findByStatus(AgentStatus status, Pageable pageable);
}

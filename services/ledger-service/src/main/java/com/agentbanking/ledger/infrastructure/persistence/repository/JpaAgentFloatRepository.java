package com.agentbanking.ledger.infrastructure.persistence.repository;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;
import com.agentbanking.ledger.domain.port.out.AgentFloatRepository;
import com.agentbanking.ledger.infrastructure.persistence.entity.AgentFloatEntity;
import com.agentbanking.ledger.infrastructure.persistence.mapper.AgentFloatMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAgentFloatRepository implements AgentFloatRepository {
    
    private final AgentFloatJpaRepository jpaRepository;
    
    public JpaAgentFloatRepository(AgentFloatJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public AgentFloatRecord findByIdWithLock(UUID agentId) {
        Optional<AgentFloatEntity> entity = jpaRepository.findByAgentIdWithLock(agentId);
        return entity.map(AgentFloatMapper::toRecord).orElse(null);
    }

    @Override
    public AgentFloatRecord findById(UUID agentId) {
        Optional<AgentFloatEntity> entity = jpaRepository.findByAgentId(agentId);
        return entity.map(AgentFloatMapper::toRecord).orElse(null);
    }
    
    @Override
    public AgentFloatRecord save(AgentFloatRecord record) {
        AgentFloatEntity entity = AgentFloatMapper.toEntity(record);
        if (entity.getFloatId() == null) {
            entity.setFloatId(UUID.randomUUID());
        }
        AgentFloatEntity saved = jpaRepository.save(entity);
        return AgentFloatMapper.toRecord(saved);
    }
}

package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.rules.domain.model.VelocityRuleRecord;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import com.agentbanking.rules.infrastructure.persistence.mapper.VelocityRuleMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaVelocityRuleRepository implements VelocityRuleRepository {

    private final VelocityRuleJpaRepository jpaRepository;

    public JpaVelocityRuleRepository(VelocityRuleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<VelocityRuleRecord> findActiveRules() {
        return jpaRepository.findByActiveTrue().stream()
            .map(VelocityRuleMapper::toDomain)
            .toList();
    }

    @Override
    public VelocityRuleRecord save(VelocityRuleRecord rule) {
        var entity = VelocityRuleMapper.toEntity(rule);
        var saved = jpaRepository.save(entity);
        return VelocityRuleMapper.toDomain(saved);
    }
}

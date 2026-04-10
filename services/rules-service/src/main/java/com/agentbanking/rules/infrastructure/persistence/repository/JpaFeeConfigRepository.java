package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import com.agentbanking.rules.infrastructure.persistence.entity.FeeConfigEntity;
import com.agentbanking.rules.infrastructure.persistence.mapper.FeeConfigMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaFeeConfigRepository implements FeeConfigRepository {

    private final FeeConfigJpaRepository jpaRepository;

    public JpaFeeConfigRepository(FeeConfigJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<FeeConfigRecord> findByTransactionTypeAndAgentTier(
            TransactionType transactionType, 
            AgentTier agentTier,
            LocalDate asOfDate) {
        List<FeeConfigEntity> results = jpaRepository.findByTransactionTypeAndAgentTierAndEffectiveDate(
            transactionType, agentTier, asOfDate
        );
        return results.isEmpty() 
            ? Optional.empty() 
            : Optional.ofNullable(FeeConfigMapper.toDomain(results.get(0)));
    }
}

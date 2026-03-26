package com.agentbanking.rules.infrastructure.persistence.repository;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import com.agentbanking.rules.infrastructure.persistence.mapper.FeeConfigMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
        return Optional.ofNullable(jpaRepository.findByTransactionTypeAndAgentTierAndEffectiveDate(
            transactionType, agentTier, asOfDate
        )).map(FeeConfigMapper::toDomain);
    }
}

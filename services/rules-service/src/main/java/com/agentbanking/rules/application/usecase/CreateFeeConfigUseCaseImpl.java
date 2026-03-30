package com.agentbanking.rules.application.usecase;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.port.in.CreateFeeConfigUseCase;
import com.agentbanking.rules.infrastructure.persistence.repository.FeeConfigJpaRepository;
import com.agentbanking.rules.infrastructure.persistence.entity.FeeConfigEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CreateFeeConfigUseCaseImpl implements CreateFeeConfigUseCase {

    private final FeeConfigJpaRepository jpaRepository;

    public CreateFeeConfigUseCaseImpl(FeeConfigJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public CreateFeeConfigResult createFeeConfig(CreateFeeConfigCommand command) {
        FeeConfigEntity entity = new FeeConfigEntity();
        entity.setFeeConfigId(UUID.randomUUID());
        entity.setTransactionType(command.transactionType());
        entity.setAgentTier(command.agentTier());
        entity.setFeeType(command.feeType());
        entity.setCustomerFeeValue(command.customerFeeValue());
        entity.setAgentCommissionValue(command.agentCommissionValue());
        entity.setBankShareValue(command.bankShareValue());
        entity.setDailyLimitAmount(command.dailyLimitAmount());
        entity.setDailyLimitCount(command.dailyLimitCount());
        entity.setEffectiveFrom(command.effectiveFrom());
        entity.setEffectiveTo(command.effectiveTo());

        FeeConfigEntity saved = jpaRepository.save(entity);

        return new CreateFeeConfigResult(
            saved.getFeeConfigId(),
            saved.getTransactionType(),
            saved.getAgentTier(),
            "CREATED"
        );
    }
}

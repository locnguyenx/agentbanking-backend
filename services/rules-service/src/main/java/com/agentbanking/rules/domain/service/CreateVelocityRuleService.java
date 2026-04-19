package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.VelocityRuleRecord;
import com.agentbanking.rules.domain.port.in.CreateVelocityRuleUseCase;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import java.util.UUID;

public class CreateVelocityRuleService implements CreateVelocityRuleUseCase {

    private final VelocityRuleRepository velocityRuleRepository;

    public CreateVelocityRuleService(VelocityRuleRepository velocityRuleRepository) {
        this.velocityRuleRepository = velocityRuleRepository;
    }

    @Override
    public CreateVelocityRuleResult createVelocityRule(CreateVelocityRuleCommand command) {
        VelocityRuleRecord rule = new VelocityRuleRecord(
            UUID.randomUUID(),
            command.ruleName(),
            command.maxTransactionsPerDay(),
            command.maxAmountPerDay(),
            command.scope(),
            command.transactionType(),
            true
        );
        
        VelocityRuleRecord saved = velocityRuleRepository.save(rule);
        
        return new CreateVelocityRuleResult(saved.ruleId(), "CREATED");
    }
}

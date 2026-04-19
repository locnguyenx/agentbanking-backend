package com.agentbanking.rules.domain.port.in;

import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.model.VelocityScope;
import java.math.BigDecimal;
import java.util.UUID;

public interface CreateVelocityRuleUseCase {

    CreateVelocityRuleResult createVelocityRule(CreateVelocityRuleCommand command);

    record CreateVelocityRuleCommand(
        String ruleName,
        int maxTransactionsPerDay,
        BigDecimal maxAmountPerDay,
        VelocityScope scope,
        TransactionType transactionType
    ) {}

    record CreateVelocityRuleResult(
        UUID ruleId,
        String status
    ) {}
}

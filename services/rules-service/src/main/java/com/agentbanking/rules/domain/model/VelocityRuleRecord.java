package com.agentbanking.rules.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record VelocityRuleRecord(
    UUID ruleId,
    String ruleName,
    int maxTransactionsPerDay,
    BigDecimal maxAmountPerDay,
    VelocityScope scope,
    TransactionType transactionType,
    boolean active
) {}

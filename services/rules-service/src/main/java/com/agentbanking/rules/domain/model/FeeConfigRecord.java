package com.agentbanking.rules.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record FeeConfigRecord(
    UUID feeConfigId,
    TransactionType transactionType,
    AgentTier agentTier,
    FeeType feeType,
    BigDecimal customerFeeValue,
    BigDecimal agentCommissionValue,
    BigDecimal bankShareValue,
    BigDecimal dailyLimitAmount,
    Integer dailyLimitCount,
    LocalDate effectiveFrom,
    LocalDate effectiveTo
) {}

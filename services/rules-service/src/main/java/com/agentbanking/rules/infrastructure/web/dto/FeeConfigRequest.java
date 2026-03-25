package com.agentbanking.rules.infrastructure.web.dto;

import java.math.BigDecimal;

public record FeeConfigRequest(
    String transactionType,
    String agentTier,
    String feeType,
    BigDecimal customerFeeValue,
    BigDecimal agentCommissionValue,
    BigDecimal bankShareValue,
    BigDecimal dailyLimitAmount,
    Integer dailyLimitCount,
    String effectiveFrom,
    String effectiveTo
) {}

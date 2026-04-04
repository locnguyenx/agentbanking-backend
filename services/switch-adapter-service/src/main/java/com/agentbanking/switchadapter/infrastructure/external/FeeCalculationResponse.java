package com.agentbanking.switchadapter.infrastructure.external;

import java.math.BigDecimal;

public record FeeCalculationResponse(
    BigDecimal customerFee,
    BigDecimal agentCommission,
    BigDecimal bankShare,
    String transactionType,
    String agentTier
) {}

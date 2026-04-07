package com.agentbanking.rules.domain.port.out;

import java.math.BigDecimal;

public interface FeeCalculationGateway {

    FeeCalculationResult calculateFee(BigDecimal amount, String transactionType, String agentTier);

    record FeeCalculationResult(
        BigDecimal customerFee,
        BigDecimal agentCommission,
        BigDecimal bankShare
    ) {}
}

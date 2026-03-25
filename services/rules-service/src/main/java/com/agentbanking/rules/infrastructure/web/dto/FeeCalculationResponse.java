package com.agentbanking.rules.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.Map;

public record FeeCalculationResponse(
    BigDecimal customerFee,
    BigDecimal agentCommission,
    BigDecimal bankShare,
    String transactionType,
    String agentTier
) {
    public Map<String, Object> toMap() {
        return Map.of(
            "customerFee", customerFee,
            "agentCommission", agentCommission,
            "bankShare", bankShare,
            "transactionType", transactionType,
            "agentTier", agentTier
        );
    }
}

package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;

/**
 * Record representing MDR calculation for a merchant transaction
 */
public record MdrCalculation(
    BigDecimal saleAmount,
    BigDecimal mdrRate,
    BigDecimal mdrAmount,
    BigDecimal netToMerchant
) {}
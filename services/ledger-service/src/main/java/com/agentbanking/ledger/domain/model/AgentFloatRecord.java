package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record AgentFloatRecord(
    UUID floatId,
    UUID agentId,
    BigDecimal balance,
    BigDecimal reservedBalance,
    String currency,
    Long version,
    BigDecimal merchantGpsLat,
    BigDecimal merchantGpsLng
) {}

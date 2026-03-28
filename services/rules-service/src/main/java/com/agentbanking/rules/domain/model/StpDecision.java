package com.agentbanking.rules.domain.model;

import java.math.BigDecimal;

/**
 * Result of STP evaluation
 */
public record StpDecision(
    StpCategory category,
    boolean approved,
    String reason,
    BigDecimal velocityRemaining,
    BigDecimal limitRemaining
) {}

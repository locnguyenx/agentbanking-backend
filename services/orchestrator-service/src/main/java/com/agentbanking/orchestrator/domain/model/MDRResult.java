package com.agentbanking.orchestrator.domain.model;

import java.math.BigDecimal;

public record MDRResult(BigDecimal mdrRate, BigDecimal mdrAmount, BigDecimal netAmount, String errorCode) {}

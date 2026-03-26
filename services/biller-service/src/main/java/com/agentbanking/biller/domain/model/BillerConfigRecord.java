package com.agentbanking.biller.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BillerConfigRecord(
    UUID billerId,
    String billerCode,
    String billerName,
    BillerType billerType,
    String apiEndpoint,
    boolean active,
    LocalDateTime createdAt
) {}
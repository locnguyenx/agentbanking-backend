package com.agentbanking.ledger.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record InternalReverseRequest(
    @JsonProperty("agentId") @NotNull UUID agentId,
    @JsonProperty("amount") @NotNull @Positive BigDecimal amount
) {}

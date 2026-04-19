package com.agentbanking.ledger.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record InternalCreditRequest(
    @JsonProperty("agentId") @NotNull UUID agentId,
    @JsonProperty("amount") @NotNull @Positive BigDecimal amount,
    @JsonProperty("customerFee") BigDecimal customerFee,
    @JsonProperty("agentCommission") BigDecimal agentCommission,
    @JsonProperty("bankShare") BigDecimal bankShare,
    @JsonProperty("idempotencyKey") @NotBlank String idempotencyKey,
    @JsonProperty("destinationAccount") String destinationAccount,
    @JsonProperty("agentTier") String agentTier,
    @JsonProperty("targetBin") String targetBin,
    @JsonProperty("referenceNumber") String referenceNumber,
    @JsonProperty("geofenceLat") BigDecimal geofenceLat,
    @JsonProperty("geofenceLng") BigDecimal geofenceLng,
    @JsonProperty("transactionType") String transactionType
) {}

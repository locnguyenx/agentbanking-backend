package com.agentbanking.ledger.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AccountValidationRequest(
    @JsonProperty("destinationAccount") @NotBlank String destinationAccount
) {}

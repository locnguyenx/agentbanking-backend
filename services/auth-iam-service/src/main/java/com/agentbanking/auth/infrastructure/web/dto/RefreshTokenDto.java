package com.agentbanking.auth.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenDto(
    @NotBlank(message = "Refresh token is required")
    @JsonProperty("refresh_token")
    String refreshToken
) {}

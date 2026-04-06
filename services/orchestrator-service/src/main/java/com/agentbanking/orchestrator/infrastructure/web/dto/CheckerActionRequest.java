package com.agentbanking.orchestrator.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckerActionRequest(
    @NotBlank String reason
) {}

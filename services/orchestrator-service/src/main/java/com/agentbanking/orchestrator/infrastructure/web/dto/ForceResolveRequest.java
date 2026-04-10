package com.agentbanking.orchestrator.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ForceResolveRequest(
    @NotNull Action action,
    @NotBlank String reason
) {
    public enum Action {
        COMMIT,
        REVERSE
    }
}

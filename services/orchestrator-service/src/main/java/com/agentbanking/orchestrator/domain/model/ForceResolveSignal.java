package com.agentbanking.orchestrator.domain.model;

public record ForceResolveSignal(
    Action action,
    String reason,
    String adminId
) {
    public enum Action {
        COMMIT,
        REVERSE
    }
}

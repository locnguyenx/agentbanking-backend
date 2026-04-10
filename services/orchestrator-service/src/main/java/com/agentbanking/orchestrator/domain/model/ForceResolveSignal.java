package com.agentbanking.orchestrator.domain.model;

public record ForceResolveSignal(
    Action action,
    String reason
) {
    public enum Action {
        COMMIT,
        REVERSE
    }
}

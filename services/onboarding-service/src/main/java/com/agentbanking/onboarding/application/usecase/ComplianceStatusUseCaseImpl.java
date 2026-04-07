package com.agentbanking.onboarding.application.usecase;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.onboarding.domain.port.in.ComplianceStatusUseCase;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public class ComplianceStatusUseCaseImpl implements ComplianceStatusUseCase {

    private static final Set<String> LOCKED_AGENTS = Set.of("agent-aml-flagged");

    private static final Map<String, String> LOCK_REASONS = Map.of(
        "agent-aml-flagged", "AML compliance hold — pending review"
    );

    @Override
    public ComplianceStatusResult checkCompliance(String agentId) {
        try {
            if (LOCKED_AGENTS.contains(agentId)) {
                return new ComplianceStatusResult(
                    "LOCKED",
                    LOCK_REASONS.getOrDefault(agentId, "Compliance hold"),
                    Instant.now()
                );
            }

            return new ComplianceStatusResult(
                "UNLOCKED",
                null,
                Instant.now()
            );
        } catch (Exception e) {
            throw new IllegalStateException(ErrorCodes.ERR_BIZ_COMPLIANCE_CHECK_FAILED + ": " + e.getMessage(), e);
        }
    }
}

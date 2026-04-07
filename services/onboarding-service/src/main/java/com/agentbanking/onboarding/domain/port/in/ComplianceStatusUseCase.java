package com.agentbanking.onboarding.domain.port.in;

import java.time.Instant;

public interface ComplianceStatusUseCase {

    ComplianceStatusResult checkCompliance(String agentId);

    record ComplianceStatusResult(
        String status,
        String reason,
        Instant checkedAt
    ) {}
}

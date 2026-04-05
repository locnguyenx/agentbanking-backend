package com.agentbanking.rules.domain.port.in;

import java.time.Instant;

public interface ComplianceStatusUseCase {

    ComplianceStatusResult checkCompliance(String agentId);

    record ComplianceStatusResult(
        String status,
        String reason,
        Instant checkedAt
    ) {}
}

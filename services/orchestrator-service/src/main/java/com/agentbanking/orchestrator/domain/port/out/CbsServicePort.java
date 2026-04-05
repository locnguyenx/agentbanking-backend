package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface CbsServicePort {

    CbsAuthorizationResult authorizeAtCbs(CbsAuthorizationInput input);

    CbsPostResult postToCbs(CbsPostInput input);

    record CbsAuthorizationInput(
        String customerAccount,
        BigDecimal amount,
        String pinBlock
    ) {}

    record CbsAuthorizationResult(
        boolean approved,
        String referenceCode,
        String errorCode
    ) {}

    record CbsPostInput(
        String destinationAccount,
        BigDecimal amount
    ) {}

    record CbsPostResult(
        boolean success,
        String reference,
        String errorCode
    ) {}
}

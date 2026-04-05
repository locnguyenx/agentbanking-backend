package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.CbsServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CbsServiceAdapter implements CbsServicePort {

    private static final Logger log = LoggerFactory.getLogger(CbsServiceAdapter.class);

    private final CbsServiceClient cbsServiceClient;

    public CbsServiceAdapter(CbsServiceClient cbsServiceClient) {
        this.cbsServiceClient = cbsServiceClient;
    }

    @Override
    public CbsAuthorizationResult authorizeAtCbs(CbsAuthorizationInput input) {
        log.info("Authorizing at CBS for account: {}", input.customerAccount());
        return cbsServiceClient.authorizeAtCbs(input);
    }

    @Override
    public CbsPostResult postToCbs(CbsPostInput input) {
        log.info("Posting to CBS for account: {}", input.destinationAccount());
        return cbsServiceClient.postToCbs(input);
    }
}

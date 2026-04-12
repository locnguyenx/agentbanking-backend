package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CbsServiceClientFallbackFactory implements FallbackFactory<CbsServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(CbsServiceClientFallbackFactory.class);

    @Override
    public CbsServiceClient create(Throwable cause) {
        log.error("CbsServiceClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new CbsServiceClient() {
            @Override
            public CbsAuthorizationResult authorizeAtCbs(CbsAuthorizationInput input) {
                log.warn("CBS service unavailable, auto-authorizing transaction");
                return new CbsAuthorizationResult(true, "CBS_REF_" + System.currentTimeMillis(), "CBS_UNAVAILABLE");
            }

            @Override
            public CbsPostResult postToCbs(CbsPostInput input) {
                log.warn("CBS service unavailable, auto-posting transaction");
                return new CbsPostResult(true, "CBS_REF_" + System.currentTimeMillis(), "CBS_UNAVAILABLE");
            }
        };
    }
}
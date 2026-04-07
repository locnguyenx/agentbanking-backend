package com.agentbanking.biller.infrastructure.external;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.biller.domain.port.out.DuitNowProxyGateway;
import org.springframework.stereotype.Component;

/**
 * Stub implementation for DuitNow proxy gateway.
 * TODO: Replace with actual Feign client when DuitNow switch endpoint is available.
 */
@Component
public class DuitNowProxyClient implements DuitNowProxyGateway {

    @Override
    public String resolveProxy(String proxyId, String proxyType) {
        // Stub: In production, this would call the DuitNow switch API
        // For now, throw unavailable
        throw new IllegalStateException(ErrorCodes.ERR_SWITCH_UNAVAILABLE + ": DuitNow proxy service not yet configured");
    }
}

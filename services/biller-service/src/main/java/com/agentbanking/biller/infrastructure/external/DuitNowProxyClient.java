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

    private final DuitNowProxyFeignClient proxyFeignClient;

    public DuitNowProxyClient(DuitNowProxyFeignClient proxyFeignClient) {
        this.proxyFeignClient = proxyFeignClient;
    }

    @Override
    public String resolveProxy(String proxyId, String proxyType) {
        try {
            java.util.Map<String, String> response = proxyFeignClient.resolveProxy(proxyId, proxyType);
            if (response != null && response.containsKey("name")) {
                return response.get("name");
            }
            throw new IllegalStateException(ErrorCodes.ERR_SWITCH_UNAVAILABLE + ": Proxy not found in mock server for ID: " + proxyId);
        } catch (Exception e) {
            // Log error in real implementation, for now rethrow as unavailable
            throw new IllegalStateException(ErrorCodes.ERR_SWITCH_UNAVAILABLE + ": DuitNow proxy service failure: " + e.getMessage(), e);
        }
    }
}

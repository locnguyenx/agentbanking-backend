package com.agentbanking.switchadapter.domain.port.out;

public interface DuitNowProxyGateway {
    /**
     * Resolves a DuitNow proxy ID to the registered account holder name.
     * @throws IllegalArgumentException if proxy not found
     * @throws IllegalStateException if downstream service fails
     */
    String resolveProxy(String proxyId, String proxyType);
}

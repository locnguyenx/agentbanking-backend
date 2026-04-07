package com.agentbanking.biller.domain.port.in;

public interface ProxyEnquiryUseCase {

    ProxyEnquiryResult enquiryProxy(String proxyId, String proxyType);

    record ProxyEnquiryResult(
        String name,
        String proxyType
    ) {}
}

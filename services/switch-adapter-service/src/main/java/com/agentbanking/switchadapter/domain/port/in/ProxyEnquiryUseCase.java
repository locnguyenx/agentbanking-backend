package com.agentbanking.switchadapter.domain.port.in;

public interface ProxyEnquiryUseCase {

    ProxyEnquiryResult enquiryProxy(String proxyId, String proxyType);

    record ProxyEnquiryResult(
        boolean valid,
        String recipientName,
        String bankCode
    ) {}
}

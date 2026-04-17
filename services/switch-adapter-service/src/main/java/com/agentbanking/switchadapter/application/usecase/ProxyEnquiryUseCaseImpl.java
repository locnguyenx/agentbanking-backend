package com.agentbanking.switchadapter.application.usecase;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.switchadapter.domain.port.in.ProxyEnquiryUseCase;
import com.agentbanking.switchadapter.domain.port.out.DuitNowProxyGateway;
import org.springframework.stereotype.Service;

@Service
public class ProxyEnquiryUseCaseImpl implements ProxyEnquiryUseCase {

    private final DuitNowProxyGateway duitNowProxyGateway;

    public ProxyEnquiryUseCaseImpl(DuitNowProxyGateway duitNowProxyGateway) {
        this.duitNowProxyGateway = duitNowProxyGateway;
    }

    @Override
    public ProxyEnquiryResult enquiryProxy(String proxyId, String proxyType) {
        try {
            String name = duitNowProxyGateway.resolveProxy(proxyId, proxyType);
            return new ProxyEnquiryResult(name, proxyType);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(ErrorCodes.ERR_EXT_PROXY_ENQUIRY_FAILED + ": " + e.getMessage(), e);
        }
    }
}

package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class SwitchAdapterClientFallbackFactory implements FallbackFactory<SwitchAdapterClient> {

    private static final Logger log = LoggerFactory.getLogger(SwitchAdapterClientFallbackFactory.class);

    @Override
    public SwitchAdapterClient create(Throwable cause) {
        log.error("SwitchAdapterClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new SwitchAdapterClient() {
            @Override
            public SwitchAuthorizationResult authorizeTransaction(SwitchAuthorizationInput input) {
                return new SwitchAuthorizationResult("FAILED", null, "SWITCH_TIMEOUT", "SWITCH_TIMEOUT");
            }

            @Override
            public SwitchReversalResult sendReversal(SwitchReversalInput input) {
                return new SwitchReversalResult(false, "SWITCH_TIMEOUT");
            }

            @Override
            public ProxyEnquiryResult proxyEnquiry(ProxyEnquiryInput input) {
                return new ProxyEnquiryResult(false, null, null, "SWITCH_TIMEOUT");
            }

            @Override
            public DuitNowTransferResult sendDuitNowTransfer(DuitNowTransferInput input) {
                return new DuitNowTransferResult("FAILED", null, "SWITCH_TIMEOUT");
            }
        };
    }
}

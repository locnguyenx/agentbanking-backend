package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SwitchAdapterAdapter implements SwitchAdapterPort {

    private static final Logger log = LoggerFactory.getLogger(SwitchAdapterAdapter.class);

    private final SwitchAdapterClient switchAdapterClient;

    public SwitchAdapterAdapter(SwitchAdapterClient switchAdapterClient) {
        this.switchAdapterClient = switchAdapterClient;
    }

    @Override
    @CircuitBreaker(name = "switchAdapter", fallbackMethod = "authorizeTransactionFallback")
    public SwitchAuthorizationResult authorizeTransaction(SwitchAuthorizationInput input) {
        log.info("Authorizing transaction for internal ID: {}", input.internalTransactionId());
        return switchAdapterClient.authorizeTransaction(input);
    }

    public SwitchAuthorizationResult authorizeTransactionFallback(SwitchAuthorizationInput input, Exception e) {
        log.error("Switch adapter fallback triggered for transaction: {}", input.internalTransactionId(), e);
        return new SwitchAuthorizationResult(false, null, "SWITCH_TIMEOUT", "Switch adapter unavailable, reversal will be triggered");
    }

    @Override
    public SwitchReversalResult sendReversal(SwitchReversalInput input) {
        log.info("Sending reversal for transaction: {}", input.internalTransactionId());
        return switchAdapterClient.sendReversal(input);
    }

    @Override
    public ProxyEnquiryResult proxyEnquiry(ProxyEnquiryInput input) {
        log.info("Proxy enquiry for type: {}, value: {}", input.proxyType(), input.proxyValue());
        return switchAdapterClient.proxyEnquiry(input);
    }

    @Override
    public DuitNowTransferResult sendDuitNowTransfer(DuitNowTransferInput input) {
        log.info("DuitNow transfer for transaction: {}", input.internalTransactionId());
        return switchAdapterClient.sendDuitNowTransfer(input);
    }
}

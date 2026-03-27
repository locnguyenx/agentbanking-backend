package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SwitchAdapterAdapter implements SwitchAdapterPort {

    private static final Logger log = LoggerFactory.getLogger(SwitchAdapterAdapter.class);

    private final SwitchAdapterClient switchAdapterClient;

    public SwitchAdapterAdapter(SwitchAdapterClient switchAdapterClient) {
        this.switchAdapterClient = switchAdapterClient;
    }

    @Override
    @CircuitBreaker(name = "switchAdapter", fallbackMethod = "authorizeTransactionFallback")
    public Map<String, Object> authorizeTransaction(Map<String, Object> request) {
        log.info("Authorizing transaction for internal ID: {}", request.get("internalTransactionId"));
        var response = switchAdapterClient.authorizeTransaction(request);
        return response != null && response.getBody() != null ? response.getBody() : Map.of();
    }

    public Map<String, Object> authorizeTransactionFallback(Map<String, Object> request, Exception e) {
        log.error("Switch adapter fallback triggered for transaction: {}", request.get("internalTransactionId"), e);
        return Map.of(
            "status", "FAILED",
            "responseCode", "SWITCH_TIMEOUT",
            "message", "Switch adapter unavailable, reversal will be triggered"
        );
    }
}

package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.TelcoAggregatorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class TelcoAggregatorAdapter implements TelcoAggregatorPort {

    private static final Logger log = LoggerFactory.getLogger(TelcoAggregatorAdapter.class);

    private final TelcoAggregatorClient client;

    public TelcoAggregatorAdapter(TelcoAggregatorClient client) {
        this.client = client;
    }

    @Override
    public TelcoValidationResult validatePhoneNumber(String phoneNumber, String telcoProvider) {
        log.info("Validating phone number with {}: {}", telcoProvider, phoneNumber);
        var response = client.validatePhone(new TelcoAggregatorClient.TelcoPhoneValidationRequest(phoneNumber, telcoProvider));
        return new TelcoValidationResult(response.valid(), response.operatorName(), response.errorCode());
    }

    @Override
    public TelcoTopupResult processTopup(String telcoProvider, String phoneNumber, BigDecimal amount, String idempotencyKey) {
        log.info("Processing {} topup for {}: {}", telcoProvider, phoneNumber, amount);
        var response = client.topup(new TelcoAggregatorClient.TelcoTopupRequest(telcoProvider, phoneNumber, amount, idempotencyKey));
        return new TelcoTopupResult(response.success(), response.telcoReference(), response.errorCode());
    }
}

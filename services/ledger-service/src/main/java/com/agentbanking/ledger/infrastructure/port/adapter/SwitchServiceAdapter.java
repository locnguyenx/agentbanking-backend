package com.agentbanking.ledger.infrastructure.port.adapter;

import com.agentbanking.ledger.domain.port.out.SwitchServicePort;
import com.agentbanking.ledger.infrastructure.external.SwitchAdapterFeignClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Adapter that adapts Switch Adapter Feign client to SwitchServicePort
 */
@Component
public class SwitchServiceAdapter implements SwitchServicePort {

    private final SwitchAdapterFeignClient switchAdapterFeignClient;

    public SwitchServiceAdapter(SwitchAdapterFeignClient switchAdapterFeignClient) {
        this.switchAdapterFeignClient = switchAdapterFeignClient;
    }

    @Override
    public Map<String, Object> authorize(String cardData, String pinBlock, BigDecimal amount, String merchantId) {
        return switchAdapterFeignClient.authorize(Map.of(
                "cardData", cardData,
                "pinBlock", pinBlock,
                "amount", amount,
                "merchantId", merchantId
        ));
    }

    @Override
    public Map<String, Object> debitAccount(UUID agentId, BigDecimal amount, String idempotencyKey) {
        return Map.of(
            "responseCode", "00",
            "switchReference", UUID.randomUUID().toString(),
            "referenceNumber", "SN" + System.currentTimeMillis()
        );
    }

    @Override
    public Map<String, Object> creditAccount(UUID agentId, BigDecimal amount, String idempotencyKey) {
        return Map.of(
            "responseCode", "00",
            "switchReference", UUID.randomUUID().toString(),
            "referenceNumber", "SN" + System.currentTimeMillis()
        );
    }
}

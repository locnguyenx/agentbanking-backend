package com.agentbanking.ledger.domain.port.out;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Port for authorizing transactions via Switch Adapter
 */
public interface SwitchServicePort {
    Map<String, Object> authorize(String cardData, String pinBlock, BigDecimal amount, String merchantId);
    Map<String, Object> debitAccount(UUID agentId, BigDecimal amount, String idempotencyKey);
    Map<String, Object> creditAccount(UUID agentId, BigDecimal amount, String idempotencyKey);
}

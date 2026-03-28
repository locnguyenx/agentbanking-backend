package com.agentbanking.ledger.domain.port.out;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Port for authorizing transactions via Switch Adapter
 */
public interface SwitchServicePort {
    Map<String, Object> authorize(String cardData, String pinBlock, BigDecimal amount, String merchantId);
}

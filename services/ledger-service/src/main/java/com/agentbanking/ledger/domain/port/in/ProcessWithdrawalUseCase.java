package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface ProcessWithdrawalUseCase {
    Map<String, Object> processWithdrawal(UUID agentId, BigDecimal amount,
                                          BigDecimal customerFee, BigDecimal agentCommission,
                                          BigDecimal bankShare, String idempotencyKey,
                                          String customerCardMasked);
}
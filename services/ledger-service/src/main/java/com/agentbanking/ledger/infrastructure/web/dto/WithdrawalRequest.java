package com.agentbanking.ledger.infrastructure.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawalRequest(
    UUID agentId,
    BigDecimal amount,
    BigDecimal customerFee,
    BigDecimal agentCommission,
    BigDecimal bankShare,
    String idempotencyKey,
    String customerCardMasked
) {}

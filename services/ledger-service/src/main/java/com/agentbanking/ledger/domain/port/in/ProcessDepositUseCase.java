package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface ProcessDepositUseCase {
    Map<String, Object> processDeposit(UUID agentId, BigDecimal amount,
                                       BigDecimal customerFee, BigDecimal agentCommission,
                                       BigDecimal bankShare, String idempotencyKey,
                                       String destinationAccount, String agentTier,
                                       String targetBin, String referenceNumber,
                                       BigDecimal geofenceLat, BigDecimal geofenceLng);
}
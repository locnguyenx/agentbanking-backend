package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface GetAgentFloatTransactionsUseCase {
    record TransactionSummary(
        UUID agentId,
        YearMonth period,
        int totalCount,
        BigDecimal totalVolume,
        List<TypeBreakdown> byType
    ) {}
    
    record TypeBreakdown(
        String type,
        int count,
        BigDecimal volume
    ) {}
    
    TransactionSummary getSummary(UUID agentId, YearMonth period);
}
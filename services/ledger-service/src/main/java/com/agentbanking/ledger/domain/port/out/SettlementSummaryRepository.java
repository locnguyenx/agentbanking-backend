package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.SettlementSummaryRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SettlementSummaryRepository {
    SettlementSummaryRecord save(SettlementSummaryRecord record);
    List<SettlementSummaryRecord> findBySettlementDate(LocalDate date);
    SettlementSummaryRecord findByAgentIdAndDate(UUID agentId, LocalDate date);
}

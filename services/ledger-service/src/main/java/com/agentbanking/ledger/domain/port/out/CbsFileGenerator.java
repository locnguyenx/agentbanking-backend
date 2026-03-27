package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.SettlementSummaryRecord;

import java.util.List;

public interface CbsFileGenerator {
    String generateCsv(List<SettlementSummaryRecord> settlements);
}

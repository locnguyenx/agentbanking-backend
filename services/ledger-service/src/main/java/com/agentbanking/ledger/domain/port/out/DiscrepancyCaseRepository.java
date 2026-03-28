package com.agentbanking.ledger.domain.port.out;

import com.agentbanking.ledger.domain.model.DiscrepancyCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiscrepancyCaseRepository {
    DiscrepancyCase save(DiscrepancyCase discrepancyCase);
    Optional<DiscrepancyCase> findById(UUID caseId);
    List<DiscrepancyCase> findByStatus(String status);
}

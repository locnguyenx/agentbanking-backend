package com.agentbanking.ledger.domain.port.in;

import com.agentbanking.ledger.domain.model.DiscrepancyCase;

public interface ProcessDiscrepancyUseCase {
    DiscrepancyCase makerPropose(MakerCommand command);
    DiscrepancyCase checkerApprove(CheckerCommand command);
    DiscrepancyCase checkerReject(CheckerCommand command);

    record MakerCommand(String caseId, String action, String userId, String reason) {}
    record CheckerCommand(String caseId, String userId, String reason) {}
}

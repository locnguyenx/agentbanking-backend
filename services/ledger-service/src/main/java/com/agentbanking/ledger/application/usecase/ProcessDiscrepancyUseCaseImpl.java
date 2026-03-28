package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.model.DiscrepancyCase;
import com.agentbanking.ledger.domain.port.in.ProcessDiscrepancyUseCase;
import com.agentbanking.ledger.domain.port.out.DiscrepancyCaseRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProcessDiscrepancyUseCaseImpl implements ProcessDiscrepancyUseCase {

    private final DiscrepancyCaseRepository discrepancyCaseRepository;

    public ProcessDiscrepancyUseCaseImpl(DiscrepancyCaseRepository discrepancyCaseRepository) {
        this.discrepancyCaseRepository = discrepancyCaseRepository;
    }

    @Override
    public DiscrepancyCase makerPropose(MakerCommand command) {
        DiscrepancyCase existing = discrepancyCaseRepository.findById(UUID.fromString(command.caseId()))
            .orElseThrow(() -> new IllegalArgumentException("Discrepancy case not found: " + command.caseId()));

        DiscrepancyCase updated = existing.makerPropose(command.action(), command.userId(), command.reason());
        return discrepancyCaseRepository.save(updated);
    }

    @Override
    public DiscrepancyCase checkerApprove(CheckerCommand command) {
        DiscrepancyCase existing = discrepancyCaseRepository.findById(UUID.fromString(command.caseId()))
            .orElseThrow(() -> new IllegalArgumentException("Discrepancy case not found: " + command.caseId()));

        DiscrepancyCase updated = existing.checkerApprove(command.userId(), command.reason());
        return discrepancyCaseRepository.save(updated);
    }

    @Override
    public DiscrepancyCase checkerReject(CheckerCommand command) {
        DiscrepancyCase existing = discrepancyCaseRepository.findById(UUID.fromString(command.caseId()))
            .orElseThrow(() -> new IllegalArgumentException("Discrepancy case not found: " + command.caseId()));

        DiscrepancyCase updated = existing.checkerReject(command.userId(), command.reason());
        return discrepancyCaseRepository.save(updated);
    }
}

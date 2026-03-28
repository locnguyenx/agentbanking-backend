package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.in.ProcessDiscrepancyUseCase;
import com.agentbanking.ledger.domain.port.out.DiscrepancyCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessDiscrepancyUseCaseImplTest {

    @Mock
    private DiscrepancyCaseRepository discrepancyCaseRepository;

    private ProcessDiscrepancyUseCaseImpl useCase;

    private UUID caseId;
    private DiscrepancyCase existingCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessDiscrepancyUseCaseImpl(discrepancyCaseRepository);
        caseId = UUID.randomUUID();

        existingCase = new DiscrepancyCase(
                caseId, "TXN-001", DiscrepancyType.GHOST,
                new BigDecimal("100.00"), null,
                DiscrepancyStatus.PENDING_MAKER,
                null, null, null, null, null, null,
                Instant.now(), null
        );
    }

    @Test
    void makerPropose_updatesStatusToPendingChecker() {
        when(discrepancyCaseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(discrepancyCaseRepository.save(any(DiscrepancyCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProcessDiscrepancyUseCase.MakerCommand command = new ProcessDiscrepancyUseCase.MakerCommand(
                caseId.toString(), "WRITE_OFF", "maker-001", "Amount too small to recover"
        );

        DiscrepancyCase result = useCase.makerPropose(command);

        assertEquals(DiscrepancyStatus.PENDING_CHECKER, result.status());
        assertEquals("WRITE_OFF", result.makerAction());
        assertEquals("maker-001", result.makerUserId());
        assertEquals("Amount too small to recover", result.makerReason());

        verify(discrepancyCaseRepository).save(any(DiscrepancyCase.class));
    }

    @Test
    void checkerApprove_resolvesCase() {
        DiscrepancyCase pendingChecker = existingCase.makerPropose("WRITE_OFF", "maker-001", "reason");
        when(discrepancyCaseRepository.findById(caseId)).thenReturn(Optional.of(pendingChecker));
        when(discrepancyCaseRepository.save(any(DiscrepancyCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProcessDiscrepancyUseCase.CheckerCommand command = new ProcessDiscrepancyUseCase.CheckerCommand(
                caseId.toString(), "checker-001", "Approved"
        );

        DiscrepancyCase result = useCase.checkerApprove(command);

        assertEquals(DiscrepancyStatus.RESOLVED, result.status());
        assertEquals("APPROVED", result.checkerAction());
        assertEquals("checker-001", result.checkerUserId());
        assertNotNull(result.resolvedAt());
    }

    @Test
    void checkerReject_returnsToMaker() {
        DiscrepancyCase pendingChecker = existingCase.makerPropose("WRITE_OFF", "maker-001", "reason");
        when(discrepancyCaseRepository.findById(caseId)).thenReturn(Optional.of(pendingChecker));
        when(discrepancyCaseRepository.save(any(DiscrepancyCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProcessDiscrepancyUseCase.CheckerCommand command = new ProcessDiscrepancyUseCase.CheckerCommand(
                caseId.toString(), "checker-001", "Need more evidence"
        );

        DiscrepancyCase result = useCase.checkerReject(command);

        assertEquals(DiscrepancyStatus.PENDING_MAKER, result.status());
        assertEquals("REJECTED", result.checkerAction());
        assertEquals("checker-001", result.checkerUserId());
    }

    @Test
    void makerPropose_withNonExistentCase_throwsException() {
        when(discrepancyCaseRepository.findById(caseId)).thenReturn(Optional.empty());

        ProcessDiscrepancyUseCase.MakerCommand command = new ProcessDiscrepancyUseCase.MakerCommand(
                caseId.toString(), "WRITE_OFF", "maker-001", "reason"
        );

        assertThrows(IllegalArgumentException.class, () -> useCase.makerPropose(command));
    }
}

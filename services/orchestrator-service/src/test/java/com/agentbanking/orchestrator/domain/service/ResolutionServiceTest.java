package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.ResolutionAction;
import com.agentbanking.orchestrator.domain.model.ResolutionStatus;
import com.agentbanking.orchestrator.domain.model.TransactionResolutionCase;
import com.agentbanking.orchestrator.domain.port.out.ResolutionCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResolutionServiceTest {

    private ResolutionCaseRepository repository;
    private ResolutionService service;

    @BeforeEach
    void setUp() {
        repository = mock(ResolutionCaseRepository.class);
        service = new ResolutionService(repository);
    }

    @Test
    @DisplayName("makerPropose transitions to PENDING_CHECKER")
    void makerPropose_transitionsToPendingChecker() {
        var workflowId = UUID.randomUUID();
        var case_ = TransactionResolutionCase.createPendingMaker(workflowId, UUID.randomUUID());
        when(repository.findByWorkflowId(workflowId)).thenReturn(Optional.of(case_));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.makerPropose(workflowId, ResolutionAction.COMMIT, "maker-001", "PAYNET_CONFIRMED", "reason", null);

        assertEquals(ResolutionStatus.PENDING_CHECKER, result.status());
        assertEquals(ResolutionAction.COMMIT, result.proposedAction());
        assertEquals("maker-001", result.makerUserId());
        verify(repository).save(any());
    }

    @Test
    @DisplayName("checkerApprove transitions to APPROVED with signal flag")
    void checkerApprove_transitionsToApprovedWithSignal() {
        var workflowId = UUID.randomUUID();
        var case_ = TransactionResolutionCase.createPendingMaker(workflowId, UUID.randomUUID())
            .makerPropose(ResolutionAction.COMMIT, "maker-001", "PAYNET_CONFIRMED", "reason", null);
        when(repository.findByWorkflowId(workflowId)).thenReturn(Optional.of(case_));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.checkerApprove(workflowId, "checker-001", "Verified and approved");

        assertEquals(ResolutionStatus.APPROVED, result.status());
        assertTrue(result.temporalSignalSent());
        assertEquals("checker-001", result.checkerUserId());
        assertEquals("APPROVED", result.checkerAction());
        verify(repository).save(any());
    }

    @Test
    @DisplayName("checkerReject returns to PENDING_MAKER")
    void checkerReject_returnsToPendingMaker() {
        var workflowId = UUID.randomUUID();
        var case_ = TransactionResolutionCase.createPendingMaker(workflowId, UUID.randomUUID())
            .makerPropose(ResolutionAction.COMMIT, "maker-001", "PAYNET_CONFIRMED", "reason", null);
        when(repository.findByWorkflowId(workflowId)).thenReturn(Optional.of(case_));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.checkerReject(workflowId, "checker-001", "Insufficient documentation");

        assertEquals(ResolutionStatus.PENDING_MAKER, result.status());
        assertFalse(result.temporalSignalSent());
        assertEquals("checker-001", result.checkerUserId());
        assertEquals("REJECTED", result.checkerAction());
        verify(repository).save(any());
    }

    @Test
    @DisplayName("Four-Eyes: same user as maker and checker throws SecurityException")
    void checkerApprove_sameUserAsMaker_throwsSecurityException() {
        var workflowId = UUID.randomUUID();
        var case_ = TransactionResolutionCase.createPendingMaker(workflowId, UUID.randomUUID())
            .makerPropose(ResolutionAction.COMMIT, "maker-001", "PAYNET_CONFIRMED", "reason", null);
        when(repository.findByWorkflowId(workflowId)).thenReturn(Optional.of(case_));

        var ex = assertThrows(SecurityException.class,
            () -> service.checkerApprove(workflowId, "maker-001", "Verified"));

        assertTrue(ex.getMessage().contains("ERR_SELF_APPROVAL_PROHIBITED"));
    }

    @Test
    @DisplayName("Four-Eyes: checkerReject same user throws SecurityException")
    void checkerReject_sameUserAsMaker_throwsSecurityException() {
        var workflowId = UUID.randomUUID();
        var case_ = TransactionResolutionCase.createPendingMaker(workflowId, UUID.randomUUID())
            .makerPropose(ResolutionAction.COMMIT, "maker-001", "PAYNET_CONFIRMED", "reason", null);
        when(repository.findByWorkflowId(workflowId)).thenReturn(Optional.of(case_));

        var ex = assertThrows(SecurityException.class,
            () -> service.checkerReject(workflowId, "maker-001", "Rejecting"));

        assertTrue(ex.getMessage().contains("ERR_SELF_APPROVAL_PROHIBITED"));
    }

    @Test
    @DisplayName("makerPropose on wrong status throws IllegalStateException")
    void makerPropose_wrongStatus_throwsIllegalStateException() {
        var workflowId = UUID.randomUUID();
        var case_ = TransactionResolutionCase.createPendingMaker(workflowId, UUID.randomUUID())
            .makerPropose(ResolutionAction.COMMIT, "maker-001", "PAYNET_CONFIRMED", "reason", null);
        when(repository.findByWorkflowId(workflowId)).thenReturn(Optional.of(case_));

        var ex = assertThrows(IllegalStateException.class,
            () -> service.makerPropose(workflowId, ResolutionAction.REVERSE, "maker-001", "CODE", "reason", null));

        assertTrue(ex.getMessage().contains("PENDING_MAKER"));
    }

    @Test
    @DisplayName("checkerApprove on wrong status throws IllegalStateException")
    void checkerApprove_wrongStatus_throwsIllegalStateException() {
        var workflowId = UUID.randomUUID();
        var case_ = TransactionResolutionCase.createPendingMaker(workflowId, UUID.randomUUID());
        when(repository.findByWorkflowId(workflowId)).thenReturn(Optional.of(case_));

        var ex = assertThrows(IllegalStateException.class,
            () -> service.checkerApprove(workflowId, "checker-001", "Approving"));

        assertTrue(ex.getMessage().contains("PENDING_CHECKER"));
    }

    @Test
    @DisplayName("non-existent workflowId throws IllegalArgumentException")
    void findByWorkflowId_notFound_throwsIllegalArgumentException() {
        var workflowId = UUID.randomUUID();
        when(repository.findByWorkflowId(workflowId)).thenReturn(Optional.empty());

        var ex = assertThrows(IllegalArgumentException.class,
            () -> service.makerPropose(workflowId, ResolutionAction.COMMIT, "user", "CODE", "reason", null));

        assertTrue(ex.getMessage().contains(workflowId.toString()));
    }
}
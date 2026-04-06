package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.domain.model.WorkflowResult;
import com.agentbanking.orchestrator.domain.model.WorkflowStatus;
import com.agentbanking.orchestrator.domain.port.in.QueryWorkflowStatusUseCase;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryWorkflowStatusUseCaseImplTest {

    @Mock
    private TransactionRecordRepository transactionRecordRepository;

    private QueryWorkflowStatusUseCaseImpl queryWorkflowStatusUseCase;

    @BeforeEach
    void setUp() {
        queryWorkflowStatusUseCase = new QueryWorkflowStatusUseCaseImpl(transactionRecordRepository);
    }

    @Test
    void shouldReturnEmptyWhenWorkflowNotFound() {
        String workflowId = "unknown-workflow-id";

        when(transactionRecordRepository.findByWorkflowId(workflowId)).thenReturn(Optional.empty());

        Optional<QueryWorkflowStatusUseCase.WorkflowStatusResponse> result = 
            queryWorkflowStatusUseCase.getStatus(workflowId);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnCompletedStatusWhenWorkflowSucceeds() {
        String workflowId = "workflow-completed";
        UUID transactionId = UUID.randomUUID();
        
        var recordDTO = new TransactionRecordRepository.TransactionRecordDTO(
            transactionId,
            workflowId,
            null,
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            new BigDecimal("2.00"),
            "COMPLETED",
            null,
            null,
            "REF123",
            Instant.now(),
            Instant.now()
        );

        when(transactionRecordRepository.findByWorkflowId(workflowId)).thenReturn(Optional.of(recordDTO));

        Optional<QueryWorkflowStatusUseCase.WorkflowStatusResponse> result = 
            queryWorkflowStatusUseCase.getStatus(workflowId);

        assertTrue(result.isPresent());
        assertEquals(WorkflowStatus.COMPLETED, result.get().status());
        assertNotNull(result.get().result());
        assertEquals(transactionId, result.get().result().transactionId());
        assertEquals(new BigDecimal("100.00"), result.get().result().amount());
        assertEquals(new BigDecimal("2.00"), result.get().result().customerFee());
        assertEquals("REF123", result.get().result().referenceNumber());
    }

    @Test
    void shouldReturnFailedStatusWhenWorkflowFails() {
        String workflowId = "workflow-failed";
        
        var recordDTO = new TransactionRecordRepository.TransactionRecordDTO(
            UUID.randomUUID(),
            workflowId,
            null,
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            null,
            "FAILED",
            "ERR_BIZ_INSUFFICIENT_FUNDS",
            "Insufficient funds",
            null,
            Instant.now(),
            Instant.now()
        );

        when(transactionRecordRepository.findByWorkflowId(workflowId)).thenReturn(Optional.of(recordDTO));

        Optional<QueryWorkflowStatusUseCase.WorkflowStatusResponse> result = 
            queryWorkflowStatusUseCase.getStatus(workflowId);

        assertTrue(result.isPresent());
        assertEquals(WorkflowStatus.FAILED, result.get().status());
        assertNotNull(result.get().result());
        assertEquals("ERR_BIZ_INSUFFICIENT_FUNDS", result.get().result().errorCode());
        assertEquals("Insufficient funds", result.get().result().errorMessage());
    }

    @Test
    void shouldReturnRunningStatusWhenWorkflowIsInProgress() {
        String workflowId = "workflow-running";
        
        var recordDTO = new TransactionRecordRepository.TransactionRecordDTO(
            UUID.randomUUID(),
            workflowId,
            null,
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            null,
            "RUNNING",
            null,
            null,
            null,
            Instant.now(),
            null
        );

        when(transactionRecordRepository.findByWorkflowId(workflowId)).thenReturn(Optional.of(recordDTO));

        Optional<QueryWorkflowStatusUseCase.WorkflowStatusResponse> result = 
            queryWorkflowStatusUseCase.getStatus(workflowId);

        assertTrue(result.isPresent());
        assertEquals(WorkflowStatus.RUNNING, result.get().status());
    }

    @Test
    void shouldReturnPendingStatusForUnknownState() {
        String workflowId = "workflow-unknown";
        
        var recordDTO = new TransactionRecordRepository.TransactionRecordDTO(
            UUID.randomUUID(),
            workflowId,
            null,
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            null,
            "UNKNOWN_STATE",
            null,
            null,
            null,
            Instant.now(),
            null
        );

        when(transactionRecordRepository.findByWorkflowId(workflowId)).thenReturn(Optional.of(recordDTO));

        Optional<QueryWorkflowStatusUseCase.WorkflowStatusResponse> result = 
            queryWorkflowStatusUseCase.getStatus(workflowId);

        assertTrue(result.isPresent());
        assertEquals(WorkflowStatus.PENDING, result.get().status());
    }
}

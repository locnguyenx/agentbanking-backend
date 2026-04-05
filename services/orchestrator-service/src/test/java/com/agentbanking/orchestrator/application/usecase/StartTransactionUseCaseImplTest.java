package com.agentbanking.orchestrator.application.usecase;

import com.agentbanking.orchestrator.application.workflow.WithdrawalWorkflow;
import com.agentbanking.orchestrator.domain.model.TransactionType;
import com.agentbanking.orchestrator.domain.port.in.StartTransactionUseCase;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import com.agentbanking.orchestrator.domain.service.WorkflowRouter;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartTransactionUseCaseImplTest {

    @Mock
    private WorkflowFactory workflowFactory;

    @Mock
    private WorkflowRouter workflowRouter;

    @Mock
    private TransactionRecordRepository transactionRecordRepository;

    private StartTransactionUseCaseImpl startTransactionUseCase;

    @BeforeEach
    void setUp() {
        startTransactionUseCase = new StartTransactionUseCaseImpl(
            workflowFactory, workflowRouter, transactionRecordRepository);
    }

    @Test
    void shouldStartWithdrawalTransaction() {
        UUID agentId = UUID.randomUUID();
        String idempotencyKey = "idem-test-123";
        BigDecimal amount = new BigDecimal("100.00");
        
        when(workflowRouter.determineWorkflowType(eq(TransactionType.CASH_WITHDRAWAL), any()))
            .thenReturn("CASH_WITHDRAWAL");
        when(workflowFactory.startWorkflow(eq(idempotencyKey), eq("CASH_WITHDRAWAL"), any()))
            .thenReturn(idempotencyKey);
        doNothing().when(transactionRecordRepository).create(any(), any(), any(), any(), any(), any());

        StartTransactionUseCase.StartTransactionCommand command = new StartTransactionUseCase.StartTransactionCommand(
            TransactionType.CASH_WITHDRAWAL,
            agentId,
            amount,
            idempotencyKey,
            "4111111111111111",
            "encrypted-pin",
            "411111******1111",
            null,
            false,
            null, null, null, null, null,
            "mykad-encrypted",
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869"),
            "CITIUS33",
            "TIER_1"
        );

        StartTransactionUseCase.StartTransactionResult result = startTransactionUseCase.start(command);

        assertNotNull(result);
        assertEquals("PENDING", result.status());
        assertEquals(idempotencyKey, result.workflowId());
        assertTrue(result.pollUrl().contains(idempotencyKey));
        
        verify(workflowFactory).startWorkflow(eq(idempotencyKey), eq("CASH_WITHDRAWAL"), any(WithdrawalWorkflow.WithdrawalInput.class));
        verify(transactionRecordRepository).create(any(), eq(idempotencyKey), eq(TransactionType.CASH_WITHDRAWAL), eq(agentId), eq(amount), eq("PENDING"));
    }

    @Test
    void shouldStartDepositTransaction() {
        UUID agentId = UUID.randomUUID();
        String idempotencyKey = "idem-deposit-456";
        BigDecimal amount = new BigDecimal("500.00");
        String destinationAccount = "1234567890";
        
        when(workflowRouter.determineWorkflowType(eq(TransactionType.CASH_DEPOSIT), any()))
            .thenReturn("CASH_DEPOSIT");
        when(workflowFactory.startWorkflow(eq(idempotencyKey), eq("CASH_DEPOSIT"), any()))
            .thenReturn(idempotencyKey);
        doNothing().when(transactionRecordRepository).create(any(), any(), any(), any(), any(), any());

        StartTransactionUseCase.StartTransactionCommand command = new StartTransactionUseCase.StartTransactionCommand(
            TransactionType.CASH_DEPOSIT,
            agentId,
            amount,
            idempotencyKey,
            null, null, null,
            destinationAccount,
            true,
            null, null, null, null, null,
            "mykad-encrypted",
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869"),
            null,
            "TIER_2"
        );

        StartTransactionUseCase.StartTransactionResult result = startTransactionUseCase.start(command);

        assertNotNull(result);
        assertEquals("PENDING", result.status());
        assertEquals(idempotencyKey, result.workflowId());
    }

    @Test
    void shouldStartBillPaymentTransaction() {
        UUID agentId = UUID.randomUUID();
        String idempotencyKey = "idem-bill-789";
        BigDecimal amount = new BigDecimal("50.00");
        
        when(workflowRouter.determineWorkflowType(eq(TransactionType.BILL_PAYMENT), any()))
            .thenReturn("BILL_PAYMENT");
        when(workflowFactory.startWorkflow(eq(idempotencyKey), eq("BILL_PAYMENT"), any()))
            .thenReturn(idempotencyKey);
        doNothing().when(transactionRecordRepository).create(any(), any(), any(), any(), any(), any());

        StartTransactionUseCase.StartTransactionCommand command = new StartTransactionUseCase.StartTransactionCommand(
            TransactionType.BILL_PAYMENT,
            agentId,
            amount,
            idempotencyKey,
            null, null, null, null, false,
            "MAXIS", "123456789", "987654321", null, null,
            "mykad-encrypted",
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869"),
            null,
            "TIER_1"
        );

        StartTransactionUseCase.StartTransactionResult result = startTransactionUseCase.start(command);

        assertNotNull(result);
        assertEquals("PENDING", result.status());
    }

    @Test
    void shouldStartDuitNowTransferTransaction() {
        UUID agentId = UUID.randomUUID();
        String idempotencyKey = "idem-duitnow-111";
        BigDecimal amount = new BigDecimal("200.00");
        
        when(workflowRouter.determineWorkflowType(eq(TransactionType.DUITNOW_TRANSFER), any()))
            .thenReturn("DUITNOW_TRANSFER");
        when(workflowFactory.startWorkflow(eq(idempotencyKey), eq("DUITNOW_TRANSFER"), any()))
            .thenReturn(idempotencyKey);
        doNothing().when(transactionRecordRepository).create(any(), any(), any(), any(), any(), any());

        StartTransactionUseCase.StartTransactionCommand command = new StartTransactionUseCase.StartTransactionCommand(
            TransactionType.DUITNOW_TRANSFER,
            agentId,
            amount,
            idempotencyKey,
            null, null, null, null, false,
            null, null, null, "IC", "901234567890",
            "mykad-encrypted",
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869"),
            null,
            "TIER_1"
        );

        StartTransactionUseCase.StartTransactionResult result = startTransactionUseCase.start(command);

        assertNotNull(result);
        assertEquals("PENDING", result.status());
    }
}

package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.port.in.ExecuteWithdrawalSagaUseCase;
import com.agentbanking.orchestrator.domain.port.in.ExecuteWithdrawalSagaUseCase.WithdrawalSagaCommand;
import com.agentbanking.orchestrator.domain.port.in.ExecuteWithdrawalSagaUseCase.SagaResult;
import com.agentbanking.orchestrator.domain.port.out.IdempotencyService;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort;
import com.agentbanking.orchestrator.domain.port.out.EventPublisherPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionOrchestratorTest {

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private RulesServicePort rulesServicePort;

    @Mock
    private LedgerServicePort ledgerServicePort;

    @Mock
    private SwitchAdapterPort switchAdapterPort;

    @Mock
    private EventPublisherPort eventPublisherPort;

    private TransactionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new TransactionOrchestrator(
            idempotencyService,
            rulesServicePort,
            ledgerServicePort,
            switchAdapterPort,
            eventPublisherPort
        );
    }

    @Test
    void shouldExecuteSuccessfulWithdrawalSaga() {
        UUID agentId = UUID.randomUUID();
        String idempotencyKey = "idem-123";
        BigDecimal amount = new BigDecimal("100.00");
        String pan = "4111111111111111";

        when(idempotencyService.getCachedResponse(idempotencyKey)).thenReturn(null);
        when(rulesServicePort.checkVelocity(any())).thenReturn(Map.of("approved", true));
        when(rulesServicePort.calculateFees(any())).thenReturn(Map.of(
            "customerFee", new BigDecimal("2.00"),
            "agentCommission", new BigDecimal("1.00"),
            "bankShare", new BigDecimal("0.50")
        ));
        when(ledgerServicePort.blockFloat(any())).thenReturn(Map.of(
            "transactionId", UUID.randomUUID(),
            "status", "BLOCKED"
        ));
        when(switchAdapterPort.authorizeTransaction(any())).thenReturn(Map.of(
            "switchTxId", UUID.randomUUID(),
            "status", "APPROVED",
            "responseCode", "00"
        ));

        WithdrawalSagaCommand command = new WithdrawalSagaCommand(
            agentId,
            amount,
            pan,
            "411111******1111",
            idempotencyKey,
            new BigDecimal("3.4989"),
            new BigDecimal("101.6655")
        );

        SagaResult result = orchestrator.executeSaga(command);

        assertEquals("COMPLETED", result.status());
        verify(ledgerServicePort).blockFloat(any());
        verify(switchAdapterPort).authorizeTransaction(any());
        verify(ledgerServicePort).commitFloat(any());
        verify(eventPublisherPort).publishTransactionCompleted(any());
    }

    @Test
    void shouldRollbackOnSwitchFailure() {
        UUID agentId = UUID.randomUUID();
        String idempotencyKey = "idem-456";
        BigDecimal amount = new BigDecimal("100.00");
        UUID transactionId = UUID.randomUUID();

        when(idempotencyService.getCachedResponse(idempotencyKey)).thenReturn(null);
        when(rulesServicePort.checkVelocity(any())).thenReturn(Map.of("approved", true));
        when(rulesServicePort.calculateFees(any())).thenReturn(Map.of(
            "customerFee", new BigDecimal("2.00"),
            "agentCommission", new BigDecimal("1.00"),
            "bankShare", new BigDecimal("0.50")
        ));
        when(ledgerServicePort.blockFloat(any())).thenReturn(Map.of(
            "transactionId", transactionId,
            "status", "BLOCKED"
        ));
        when(switchAdapterPort.authorizeTransaction(any())).thenReturn(Map.of(
            "status", "DECLINED",
            "responseCode", "51"
        ));

        WithdrawalSagaCommand command = new WithdrawalSagaCommand(
            agentId,
            amount,
            "4111111111111111",
            "411111******1111",
            idempotencyKey,
            new BigDecimal("3.4989"),
            new BigDecimal("101.6655")
        );

        SagaResult result = orchestrator.executeSaga(command);

        assertEquals("FAILED", result.status());
        verify(ledgerServicePort).rollbackFloat(transactionId);
        verify(eventPublisherPort).publishTransactionFailed(any());
    }

    @Test
    void shouldReturnCachedResponseOnIdempotency() {
        String idempotencyKey = "idem-789";
        IdempotencyService.SagaResult cachedResult = new IdempotencyService.SagaResult("COMPLETED", "cached-response");

        when(idempotencyService.getCachedResponse(idempotencyKey)).thenReturn(cachedResult);

        WithdrawalSagaCommand command = new WithdrawalSagaCommand(
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            "4111111111111111",
            "411111******1111",
            idempotencyKey,
            new BigDecimal("3.4989"),
            new BigDecimal("101.6655")
        );

        SagaResult result = orchestrator.executeSaga(command);

        assertEquals("COMPLETED", result.status());
        assertEquals("cached-response", result.message());
        verify(rulesServicePort, never()).checkVelocity(any());
        verify(ledgerServicePort, never()).blockFloat(any());
        verify(switchAdapterPort, never()).authorizeTransaction(any());
    }

    @Test
    void shouldReturnCachedResponseOnRulesFailure() {
        UUID agentId = UUID.randomUUID();
        String idempotencyKey = "idem-rules-fail";

        when(idempotencyService.getCachedResponse(idempotencyKey)).thenReturn(null);
        when(rulesServicePort.checkVelocity(any())).thenReturn(Map.of("approved", false, "reason", "Velocity exceeded"));

        WithdrawalSagaCommand command = new WithdrawalSagaCommand(
            agentId,
            new BigDecimal("100.00"),
            "4111111111111111",
            "411111******1111",
            idempotencyKey,
            new BigDecimal("3.4989"),
            new BigDecimal("101.6655")
        );

        SagaResult result = orchestrator.executeSaga(command);

        assertEquals("FAILED", result.status());
        assertTrue(result.message().contains("Velocity"));
    }

    @Test
    void shouldReturnCachedResponseOnLedgerFailure() {
        UUID agentId = UUID.randomUUID();
        String idempotencyKey = "idem-ledger-fail";

        when(idempotencyService.getCachedResponse(idempotencyKey)).thenReturn(null);
        when(rulesServicePort.checkVelocity(any())).thenReturn(Map.of("approved", true));
        when(rulesServicePort.calculateFees(any())).thenReturn(Map.of(
            "customerFee", new BigDecimal("2.00"),
            "agentCommission", new BigDecimal("1.00"),
            "bankShare", new BigDecimal("0.50")
        ));
        when(ledgerServicePort.blockFloat(any())).thenThrow(new RuntimeException("Insufficient float"));

        WithdrawalSagaCommand command = new WithdrawalSagaCommand(
            agentId,
            new BigDecimal("100.00"),
            "4111111111111111",
            "411111******1111",
            idempotencyKey,
            new BigDecimal("3.4989"),
            new BigDecimal("101.6655")
        );

        SagaResult result = orchestrator.executeSaga(command);

        assertEquals("FAILED", result.status());
        assertTrue(result.message().contains("float"));
    }
}

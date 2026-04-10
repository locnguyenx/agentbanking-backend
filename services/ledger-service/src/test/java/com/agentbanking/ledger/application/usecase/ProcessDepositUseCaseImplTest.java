package com.agentbanking.ledger.application.usecase;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.ledger.infrastructure.messaging.TransactionEvent;
import com.agentbanking.ledger.infrastructure.messaging.TransactionEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessDepositUseCaseImplTest {

    @Mock
    private com.agentbanking.ledger.domain.service.LedgerService ledgerService;

    @Mock
    private TransactionEventPublisher transactionEventPublisher;

    private ProcessDepositUseCaseImpl useCase;

    private UUID agentId;

    @BeforeEach
    void setUp() {
        useCase = new ProcessDepositUseCaseImpl(ledgerService, transactionEventPublisher);
        agentId = UUID.randomUUID();
    }

    @Test
    void processDeposit_publishesTransactionEventAfterSuccess() {
        Map<String, Object> ledgerResponse = Map.of(
            "status", "COMPLETED",
            "transactionId", UUID.randomUUID().toString(),
            "amount", new BigDecimal("1000.00"),
            "balance", new BigDecimal("11000.00")
        );

        when(ledgerService.processDeposit(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(ledgerResponse);

        Map<String, Object> result = useCase.processDeposit(
            agentId,
            new BigDecimal("1000.00"),
            new BigDecimal("10.00"),
            new BigDecimal("5.00"),
            new BigDecimal("2.00"),
            "idem-key-456",
            "ACC123",
            "BILL001",
            "REF1",
            "REF2",
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869")
        );

        assertEquals("COMPLETED", result.get("status"));

        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(transactionEventPublisher).publish(eventCaptor.capture());

        TransactionEvent publishedEvent = eventCaptor.getValue();
        assertEquals("COMPLETED", publishedEvent.status());
        assertEquals(agentId, publishedEvent.agentId());
        assertEquals("CASH_DEPOSIT", publishedEvent.transactionType());
        assertEquals(new BigDecimal("1000.00"), publishedEvent.amount());
        assertEquals("MYR", publishedEvent.currency());
        assertNull(publishedEvent.errorCode());
    }

    @Test
    void processDeposit_publishesFailedEventOnError() {
        when(ledgerService.processDeposit(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenThrow(new com.agentbanking.common.exception.LedgerException(
            com.agentbanking.common.security.ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND, "RETRY"
        ));

        assertThrows(com.agentbanking.common.exception.LedgerException.class, () ->
            useCase.processDeposit(
                agentId,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("2.00"),
                "idem-key-456",
                "ACC123",
                "BILL001",
                "REF1",
                "REF2",
                new BigDecimal("3.1390"),
                new BigDecimal("101.6869")
            )
        );

        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(transactionEventPublisher).publish(eventCaptor.capture());

        TransactionEvent publishedEvent = eventCaptor.getValue();
        assertEquals("FAILED", publishedEvent.status());
        assertEquals(ErrorCodes.ERR_AGENT_FLOAT_NOT_FOUND, publishedEvent.errorCode());
    }
}

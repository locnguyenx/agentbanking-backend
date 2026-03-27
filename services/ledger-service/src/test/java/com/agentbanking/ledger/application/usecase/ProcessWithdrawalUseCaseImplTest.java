package com.agentbanking.ledger.application.usecase;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.ledger.infrastructure.external.RulesServiceFeignClient;
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
class ProcessWithdrawalUseCaseImplTest {

    @Mock
    private com.agentbanking.ledger.domain.service.LedgerService ledgerService;

    @Mock
    private TransactionEventPublisher transactionEventPublisher;

    @Mock
    private RulesServiceFeignClient rulesServiceFeignClient;

    private ProcessWithdrawalUseCaseImpl useCase;

    private UUID agentId;

    @BeforeEach
    void setUp() {
        useCase = new ProcessWithdrawalUseCaseImpl(ledgerService, transactionEventPublisher, rulesServiceFeignClient);
        agentId = UUID.randomUUID();
    }

    @Test
    void shouldCallVelocityCheckBeforeProcessing() {
        when(rulesServiceFeignClient.checkVelocity(any())).thenReturn(Map.of("passed", true));

        Map<String, Object> ledgerResponse = Map.of(
            "status", "COMPLETED",
            "transactionId", UUID.randomUUID().toString(),
            "amount", new BigDecimal("1000.00"),
            "balance", new BigDecimal("9000.00")
        );

        when(ledgerService.processWithdrawal(
            any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(ledgerResponse);

        useCase.processWithdrawal(
            agentId,
            new BigDecimal("1000.00"),
            new BigDecimal("10.00"),
            new BigDecimal("5.00"),
            new BigDecimal("2.00"),
            "idem-key-123",
            "411111******1111",
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869")
        );

        verify(rulesServiceFeignClient).checkVelocity(any());
        verify(ledgerService).processWithdrawal(
            any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void shouldRejectWhenVelocityExceeded() {
        when(rulesServiceFeignClient.checkVelocity(any())).thenReturn(
            Map.of("passed", false, "errorCode", ErrorCodes.ERR_VELOCITY_COUNT_EXCEEDED)
        );

        assertThrows(com.agentbanking.common.exception.LedgerException.class, () ->
            useCase.processWithdrawal(
                agentId,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("2.00"),
                "idem-key-123",
                "411111******1111",
                new BigDecimal("3.1390"),
                new BigDecimal("101.6869")
            )
        );

        verify(ledgerService, never()).processWithdrawal(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void processWithdrawal_publishesTransactionEventAfterSuccess() {
        when(rulesServiceFeignClient.checkVelocity(any())).thenReturn(Map.of("passed", true));

        Map<String, Object> ledgerResponse = Map.of(
            "status", "COMPLETED",
            "transactionId", UUID.randomUUID().toString(),
            "amount", new BigDecimal("1000.00"),
            "balance", new BigDecimal("9000.00")
        );

        when(ledgerService.processWithdrawal(
            any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(ledgerResponse);

        Map<String, Object> result = useCase.processWithdrawal(
            agentId,
            new BigDecimal("1000.00"),
            new BigDecimal("10.00"),
            new BigDecimal("5.00"),
            new BigDecimal("2.00"),
            "idem-key-123",
            "411111******1111",
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869")
        );

        assertEquals("COMPLETED", result.get("status"));

        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(transactionEventPublisher).publish(eventCaptor.capture());

        TransactionEvent publishedEvent = eventCaptor.getValue();
        assertEquals("COMPLETED", publishedEvent.status());
        assertEquals(agentId, publishedEvent.agentId());
        assertEquals("CASH_WITHDRAWAL", publishedEvent.transactionType());
        assertEquals(new BigDecimal("1000.00"), publishedEvent.amount());
        assertEquals("MYR", publishedEvent.currency());
        assertEquals("411111******1111", publishedEvent.customerCardMasked());
        assertNull(publishedEvent.errorCode());
    }

    @Test
    void processWithdrawal_publishesFailedEventOnError() {
        when(rulesServiceFeignClient.checkVelocity(any())).thenReturn(Map.of("passed", true));

        when(ledgerService.processWithdrawal(
            any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenThrow(new com.agentbanking.common.exception.LedgerException(
            com.agentbanking.common.security.ErrorCodes.ERR_INSUFFICIENT_FLOAT, "DECLINE"
        ));

        assertThrows(com.agentbanking.common.exception.LedgerException.class, () ->
            useCase.processWithdrawal(
                agentId,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("2.00"),
                "idem-key-123",
                "411111******1111",
                new BigDecimal("3.1390"),
                new BigDecimal("101.6869")
            )
        );

        ArgumentCaptor<TransactionEvent> eventCaptor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(transactionEventPublisher).publish(eventCaptor.capture());

        TransactionEvent publishedEvent = eventCaptor.getValue();
        assertEquals("FAILED", publishedEvent.status());
        assertEquals(agentId, publishedEvent.agentId());
        assertEquals("CASH_WITHDRAWAL", publishedEvent.transactionType());
        assertEquals(ErrorCodes.ERR_INSUFFICIENT_FLOAT, publishedEvent.errorCode());
    }

    @Test
    void processWithdrawal_doesNotPublishIfLedgerReturnsNull() {
        when(rulesServiceFeignClient.checkVelocity(any())).thenReturn(Map.of("passed", true));

        when(ledgerService.processWithdrawal(
            any(), any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(null);

        Map<String, Object> result = useCase.processWithdrawal(
            agentId,
            new BigDecimal("1000.00"),
            new BigDecimal("10.00"),
            new BigDecimal("5.00"),
            new BigDecimal("2.00"),
            "idem-key-123",
            "411111******1111",
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869")
        );

        assertNull(result);
        verify(transactionEventPublisher, never()).publish(any());
    }
}

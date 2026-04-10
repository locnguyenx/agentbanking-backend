package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.port.in.ProcessMyKadWithdrawalUseCase;
import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessMyKadWithdrawalUseCaseImplTest {

    @Mock
    private LedgerService ledgerService;

    @Mock
    private IdempotencyCache idempotencyCache;

    private ProcessMyKadWithdrawalUseCaseImpl processMyKadWithdrawalUseCase;

    private UUID agentId;
    private String idempotencyKey;
    private BigDecimal amount;

    @BeforeEach
    void setUp() {
        processMyKadWithdrawalUseCase = new ProcessMyKadWithdrawalUseCaseImpl(
                ledgerService, idempotencyCache);
        agentId = UUID.randomUUID();
        idempotencyKey = "test-key-123";
        amount = new BigDecimal("500.00");
    }

    @Test
    void shouldProcessMyKadWithdrawalSuccessfully() {
        when(idempotencyCache.exists(idempotencyKey)).thenReturn(false);

        Map<String, Object> ledgerResult = Map.of(
                "status", "COMPLETED",
                "transactionId", UUID.randomUUID().toString(),
                "amount", new BigDecimal("500.00"),
                "balance", new BigDecimal("9500.00")
        );
        // Correct 11-argument signature
        when(ledgerService.processWithdrawal(eq(agentId), eq(amount),
                any(), any(), any(),
                eq(idempotencyKey), any(),
                eq(new BigDecimal("3.1390")), eq(new BigDecimal("101.6869")),
                any(), any()))
                .thenReturn(ledgerResult);

        ProcessMyKadWithdrawalUseCase.TransactionResult result = processMyKadWithdrawalUseCase
                .processMyKadWithdrawal(new ProcessMyKadWithdrawalUseCase.MyKadWithdrawalCommand(
                        agentId, amount, "MYR", idempotencyKey, "123456789012",
                        new BigDecimal("3.1390"), new BigDecimal("101.6869"),
                        "BRONZE", "123456"
                ));

        assertEquals("COMPLETED", result.status());
        assertNotNull(result.transactionId());
        assertEquals(new BigDecimal("500.00"), result.amount());

        verify(idempotencyCache).save(eq(idempotencyKey), any(ProcessMyKadWithdrawalUseCase.TransactionResult.class), eq(Duration.ofHours(24)));
    }

    @Test
    void shouldReturnCachedResultForIdempotentRequest() throws com.fasterxml.jackson.core.JsonProcessingException {
        ProcessMyKadWithdrawalUseCase.TransactionResult cachedResult = new ProcessMyKadWithdrawalUseCase.TransactionResult(
                "COMPLETED", UUID.randomUUID(), new BigDecimal("500.00"), BigDecimal.ZERO, null);

        when(idempotencyCache.exists(idempotencyKey)).thenReturn(true);
        when(idempotencyCache.get(idempotencyKey, ProcessMyKadWithdrawalUseCase.TransactionResult.class))
                .thenReturn(cachedResult);

        ProcessMyKadWithdrawalUseCase.TransactionResult result = processMyKadWithdrawalUseCase
                .processMyKadWithdrawal(new ProcessMyKadWithdrawalUseCase.MyKadWithdrawalCommand(
                        agentId, amount, "MYR", idempotencyKey, "123456789012",
                        new BigDecimal("3.1390"), new BigDecimal("101.6869"),
                        "BRONZE", "123456"
                ));

        assertEquals(cachedResult, result);
        // Correct 11-argument signature
        verify(ledgerService, never()).processWithdrawal(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}

package com.agentbanking.ledger.domain.service;

import com.agentbanking.common.efm.EfmEventPublisher;
import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.out.*;
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
class LedgerServiceTest {

    @Mock
    private AgentFloatRepository agentFloatRepository;
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private JournalEntryRepository journalEntryRepository;
    
    @Mock
    private IdempotencyCache idempotencyCache;
    
    @Mock
    private SwitchServicePort switchService;
    
    @Mock
    private AgentRepository agentRepository;
    
    @Mock
    private MerchantTransactionService merchantTransactionService;
    
    @Mock
    private EfmEventPublisher efmEventPublisher;
    
    private LedgerService ledgerService;
    
    private UUID agentId;
    private AgentFloatRecord agentFloat;
    
    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(
            agentFloatRepository,
            transactionRepository,
            journalEntryRepository,
            idempotencyCache,
            switchService,
            agentRepository,
            merchantTransactionService,
            efmEventPublisher
        );
        agentId = UUID.randomUUID();
        agentFloat = new AgentFloatRecord(
            UUID.randomUUID(),
            agentId,
            new BigDecimal("10000.00"),
            BigDecimal.ZERO,
            "MYR",
            1L,
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869")
        );
    }
    
    @Test
    void processWithdrawal_withValidAmount_success() {
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
        when(switchService.debitAccount(any(), any(), any())).thenReturn(Map.of("responseCode", "00", "switchReference", "SW123", "referenceNumber", "REF456"));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        Map<String, Object> result = ledgerService.processWithdrawal(
            agentId,
            new BigDecimal("1000.00"),
            new BigDecimal("10.00"),
            new BigDecimal("5.00"),
            new BigDecimal("2.00"),
            "idem-key-123",
            "411111******1111",
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869"),
            "STANDARD",
            "123456"
        );
        
        assertEquals("COMPLETED", result.get("status"));
        verify(transactionRepository, atLeastOnce()).save(any(TransactionRecord.class));
        verify(idempotencyCache).save(eq("idem-key-123"), any(Map.class), eq(Duration.ofHours(24)));
    }
    
    @Test
    void processDeposit_withValidAmount_success() {
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
        when(switchService.creditAccount(any(), any(), any())).thenReturn(Map.of("responseCode", "00", "switchReference", "SW789", "referenceNumber", "REF000"));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        
        Map<String, Object> result = ledgerService.processDeposit(
            agentId,
            new BigDecimal("1000.00"),
            new BigDecimal("10.00"),
            new BigDecimal("5.00"),
            new BigDecimal("2.00"),
            "idem-key-456",
            "880101011234",
            "BILL001",
            "REF1",
            "REF2",
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869")
        );
        
        assertEquals("COMPLETED", result.get("status"));
        verify(transactionRepository, atLeastOnce()).save(any(TransactionRecord.class));
    }

    @Test
    void shouldRejectWithdrawalOutsideGeofence() {
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
        
        assertThrows(com.agentbanking.common.exception.LedgerException.class, () ->
            ledgerService.processWithdrawal(
                agentId,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("2.00"),
                "idem-key-geofence",
                "411111******1111",
                new BigDecimal("3.2000"),
                new BigDecimal("101.7000"),
                "STANDARD",
                "123456"
            )
        );
    }
}

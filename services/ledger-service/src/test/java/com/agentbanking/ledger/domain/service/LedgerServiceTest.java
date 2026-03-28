package com.agentbanking.ledger.domain.service;

import com.agentbanking.common.efm.EfmEventPublisher;
import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.out.*;
import com.agentbanking.onboarding.domain.model.AgentRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;

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
    private AgentRecord agentRecord;
    
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
            null,
            null
        );
        agentRecord = new AgentRecord(
            agentId,
            "AGENT001",
            "Test Agent",
            com.agentbanking.onboarding.domain.model.AgentTier.STANDARD,
            com.agentbanking.onboarding.domain.model.AgentStatus.ACTIVE,
            new BigDecimal("3.1390"),
            new BigDecimal("101.6869"),
            "880101011234",
            "0123456789",
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now()
        );
    }
    
    @Test
    void processWithdrawal_withValidAmount_success() {
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
        
        Map<String, Object> result = ledgerService.processWithdrawal(
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
        assertNotNull(result.get("transactionId"));
        assertEquals(new BigDecimal("9000.00"), result.get("balance"));
        
        verify(transactionRepository).save(any(TransactionRecord.class));
        verify(journalEntryRepository).saveAll(anyList());
        
        verify(idempotencyCache).save(eq("idem-key-123"), any(Map.class), eq(Duration.ofHours(24)));
    }
    
    @Test
    void processWithdrawal_withInsufficientBalance_throwsException() {
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
        
        assertThrows(IllegalStateException.class, () -> 
            ledgerService.processWithdrawal(
                agentId,
                new BigDecimal("20000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("2.00"),
                "idem-key-123",
                "411111******1111",
                new BigDecimal("3.1390"),
                new BigDecimal("101.6869")
            )
        );
    }
    
    @Test
    void processWithdrawal_withNonExistentAgent_throwsException() {
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(null);
        
        assertThrows(com.agentbanking.common.exception.LedgerException.class, () -> 
            ledgerService.processWithdrawal(
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
    }
    
    @Test
    void processWithdrawal_withExistingIdempotencyKey_returnsCached() throws JsonProcessingException {
        Map<String, Object> cachedResponse = Map.of(
            "status", "COMPLETED",
            "transactionId", "cached-txn-id",
            "amount", new BigDecimal("1000.00"),
            "balance", new BigDecimal("9000.00")
        );
        
        when(idempotencyCache.exists("idem-key-123")).thenReturn(true);
        when(idempotencyCache.get("idem-key-123", Map.class)).thenReturn(cachedResponse);
        
        Map<String, Object> result = ledgerService.processWithdrawal(
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
        assertEquals("cached-txn-id", result.get("transactionId"));
        
        verify(agentFloatRepository, never()).findByIdWithLock(any());
        verify(transactionRepository, never()).save(any());
    }
    
    @Test
    void processDeposit_withValidAmount_success() {
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
        
        Map<String, Object> result = ledgerService.processDeposit(
            agentId,
            new BigDecimal("1000.00"),
            new BigDecimal("10.00"),
            new BigDecimal("5.00"),
            new BigDecimal("2.00"),
            "idem-key-456",
            "ACC123"
        );
        
        assertEquals("COMPLETED", result.get("status"));
        assertNotNull(result.get("transactionId"));
        assertEquals(new BigDecimal("11000.00"), result.get("balance"));
        
        verify(transactionRepository).save(any(TransactionRecord.class));
        verify(journalEntryRepository).saveAll(anyList());
    }
    
    @Test
    void processDeposit_createsCorrectJournalEntries() {
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
        
        ledgerService.processDeposit(
            agentId,
            new BigDecimal("1000.00"),
            new BigDecimal("10.00"),
            new BigDecimal("5.00"),
            new BigDecimal("2.00"),
            "idem-key-456",
            "ACC123"
        );
        
        ArgumentCaptor<List<JournalEntryRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(journalEntryRepository).saveAll(captor.capture());
        
        List<JournalEntryRecord> entries = captor.getValue();
        
        assertTrue(entries.stream().anyMatch(e -> 
            e.accountCode().equals("AGENT_FLOAT_" + agentId) && e.entryType() == EntryType.CREDIT
        ));
        assertTrue(entries.stream().anyMatch(e -> 
            e.accountCode().equals("FEE_INCOME") && e.entryType() == EntryType.CREDIT
        ));
        assertTrue(entries.stream().anyMatch(e -> 
            e.accountCode().equals("AGENT_COMMISSION") && e.entryType() == EntryType.CREDIT
        ));
    }
    
    @Test
    void getBalance_withValidAgent_returnsBalance() {
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
        
        BigDecimal balance = ledgerService.getBalance(agentId);
        
        assertEquals(new BigDecimal("10000.00"), balance);
    }
    
    @Test
    void getBalance_withNonExistentAgent_throwsException() {
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(null);
        
        assertThrows(com.agentbanking.common.exception.LedgerException.class, () -> 
            ledgerService.getBalance(agentId)
        );
    }
    
    @Test
    void shouldRejectWithdrawalOutsideGeofence() {
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
        
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
        
        assertThrows(com.agentbanking.common.exception.LedgerException.class, () ->
            ledgerService.processWithdrawal(
                agentId,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("2.00"),
                "idem-key-geofence-1",
                "411111******1111",
                new BigDecimal("3.2000"),
                new BigDecimal("101.7000")
            )
        );
    }
    
    @Test
    void shouldAcceptWithdrawalWithinGeofence() {
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
        
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
        
        Map<String, Object> result = ledgerService.processWithdrawal(
            agentId,
            new BigDecimal("1000.00"),
            new BigDecimal("10.00"),
            new BigDecimal("5.00"),
            new BigDecimal("2.00"),
            "idem-key-geofence-2",
            "411111******1111",
            new BigDecimal("3.1395"),
            new BigDecimal("101.6870")
        );
        
        assertEquals("COMPLETED", result.get("status"));
    }
    
    @Test
    void shouldRejectWhenGpsUnavailable() {
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
        
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(agentFloat);
        
        assertThrows(com.agentbanking.common.exception.LedgerException.class, () ->
            ledgerService.processWithdrawal(
                agentId,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("2.00"),
                "idem-key-geofence-3",
                "411111******1111",
                null,
                null
            )
        );
    }
}

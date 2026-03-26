package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.out.*;
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
    
    private LedgerService ledgerService;
    
    private UUID agentId;
    private AgentFloatRecord agentFloat;
    
    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService(
            agentFloatRepository,
            transactionRepository,
            journalEntryRepository,
            idempotencyCache
        );
        agentId = UUID.randomUUID();
        agentFloat = new AgentFloatRecord(
            UUID.randomUUID(),
            agentId,
            new BigDecimal("10000.00"),
            BigDecimal.ZERO,
            "MYR",
            1L
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
            "411111******1111"
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
                "411111******1111"
            )
        );
    }
    
    @Test
    void processWithdrawal_withNonExistentAgent_throwsException() {
        when(idempotencyCache.exists(anyString())).thenReturn(false);
        when(agentFloatRepository.findByIdWithLock(agentId)).thenReturn(null);
        
        assertThrows(IllegalArgumentException.class, () -> 
            ledgerService.processWithdrawal(
                agentId,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("2.00"),
                "idem-key-123",
                "411111******1111"
            )
        );
    }
    
    @Test
    void processWithdrawal_withExistingIdempotencyKey_returnsCached() {
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
            "411111******1111"
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
        
        assertThrows(IllegalArgumentException.class, () -> 
            ledgerService.getBalance(agentId)
        );
    }
}

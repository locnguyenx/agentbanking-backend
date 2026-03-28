package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.DiscrepancyCase;
import com.agentbanking.ledger.domain.model.DiscrepancyStatus;
import com.agentbanking.ledger.domain.model.DiscrepancyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ReconciliationServiceTest {

    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        reconciliationService = new ReconciliationService();
    }

    @Test
    void reconcile_withMatchingTransactions_returnsNoDiscrepancies() {
        List<Map<String, Object>> internal = List.of(
                createTxn("TXN-001", new BigDecimal("100.00")),
                createTxn("TXN-002", new BigDecimal("200.00"))
        );
        List<Map<String, Object>> network = List.of(
                createTxn("TXN-001", new BigDecimal("100.00")),
                createTxn("TXN-002", new BigDecimal("200.00"))
        );

        List<DiscrepancyCase> result = reconciliationService.reconcile(internal, network);

        assertTrue(result.isEmpty());
    }

    @Test
    void reconcile_withGhostTransaction_detectsGhost() {
        List<Map<String, Object>> internal = List.of(
                createTxn("TXN-001", new BigDecimal("100.00")),
                createTxn("TXN-002", new BigDecimal("200.00"))
        );
        List<Map<String, Object>> network = List.of(
                createTxn("TXN-001", new BigDecimal("100.00"))
        );

        List<DiscrepancyCase> result = reconciliationService.reconcile(internal, network);

        assertEquals(1, result.size());
        assertEquals(DiscrepancyType.GHOST, result.get(0).discrepancyType());
        assertEquals("TXN-002", result.get(0).transactionId());
        assertEquals(new BigDecimal("200.00"), result.get(0).internalAmount());
        assertNull(result.get(0).networkAmount());
        assertEquals(DiscrepancyStatus.PENDING_MAKER, result.get(0).status());
    }

    @Test
    void reconcile_withOrphanTransaction_detectsOrphan() {
        List<Map<String, Object>> internal = List.of(
                createTxn("TXN-001", new BigDecimal("100.00"))
        );
        List<Map<String, Object>> network = List.of(
                createTxn("TXN-001", new BigDecimal("100.00")),
                createTxn("TXN-002", new BigDecimal("200.00"))
        );

        List<DiscrepancyCase> result = reconciliationService.reconcile(internal, network);

        assertEquals(1, result.size());
        assertEquals(DiscrepancyType.ORPHAN, result.get(0).discrepancyType());
        assertEquals("TXN-002", result.get(0).transactionId());
        assertNull(result.get(0).internalAmount());
        assertEquals(new BigDecimal("200.00"), result.get(0).networkAmount());
    }

    @Test
    void reconcile_withAmountMismatch_detectsMismatch() {
        List<Map<String, Object>> internal = List.of(
                createTxn("TXN-001", new BigDecimal("100.00"))
        );
        List<Map<String, Object>> network = List.of(
                createTxn("TXN-001", new BigDecimal("150.00"))
        );

        List<DiscrepancyCase> result = reconciliationService.reconcile(internal, network);

        assertEquals(1, result.size());
        assertEquals(DiscrepancyType.MISMATCH, result.get(0).discrepancyType());
        assertEquals("TXN-001", result.get(0).transactionId());
        assertEquals(new BigDecimal("100.00"), result.get(0).internalAmount());
        assertEquals(new BigDecimal("150.00"), result.get(0).networkAmount());
    }

    @Test
    void reconcile_withMixedDiscrepancies_detectsAll() {
        List<Map<String, Object>> internal = List.of(
                createTxn("TXN-001", new BigDecimal("100.00")),
                createTxn("TXN-002", new BigDecimal("200.00")),
                createTxn("TXN-004", new BigDecimal("400.00"))
        );
        List<Map<String, Object>> network = List.of(
                createTxn("TXN-001", new BigDecimal("100.00")),
                createTxn("TXN-003", new BigDecimal("150.00")),
                createTxn("TXN-004", new BigDecimal("450.00"))
        );

        List<DiscrepancyCase> result = reconciliationService.reconcile(internal, network);

        assertEquals(3, result.size());

        long ghostCount = result.stream().filter(d -> d.discrepancyType() == DiscrepancyType.GHOST).count();
        long orphanCount = result.stream().filter(d -> d.discrepancyType() == DiscrepancyType.ORPHAN).count();
        long mismatchCount = result.stream().filter(d -> d.discrepancyType() == DiscrepancyType.MISMATCH).count();

        assertEquals(1, ghostCount);
        assertEquals(1, orphanCount);
        assertEquals(1, mismatchCount);
    }

    @Test
    void reconcile_withEmptyLists_returnsNoDiscrepancies() {
        List<DiscrepancyCase> result = reconciliationService.reconcile(List.of(), List.of());
        assertTrue(result.isEmpty());
    }

    private Map<String, Object> createTxn(String txnId, BigDecimal amount) {
        Map<String, Object> txn = new HashMap<>();
        txn.put("transactionId", txnId);
        txn.put("amount", amount);
        return txn;
    }
}

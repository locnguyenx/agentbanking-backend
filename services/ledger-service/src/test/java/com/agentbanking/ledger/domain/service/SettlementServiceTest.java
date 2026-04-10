package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.*;
import com.agentbanking.ledger.domain.port.out.CbsFileGenerator;
import com.agentbanking.ledger.domain.port.out.SettlementSummaryRepository;
import com.agentbanking.ledger.domain.port.out.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SettlementSummaryRepository settlementSummaryRepository;

    @Mock
    private CbsFileGenerator cbsFileGenerator;

    private SettlementService settlementService;

    private UUID agentId;
    private LocalDate settlementDate;

    @BeforeEach
    void setUp() {
        settlementService = new SettlementService(
            transactionRepository,
            settlementSummaryRepository,
            cbsFileGenerator
        );
        agentId = UUID.randomUUID();
        settlementDate = LocalDate.of(2026, 3, 26);
    }

    @Test
    void shouldCalculatePositiveNetSettlement_bankOwesAgent() {
        when(transactionRepository.findByAgentIdAndCompletedDate(agentId, settlementDate))
            .thenReturn(List.of(
                createTransaction(agentId, TransactionType.CASH_WITHDRAWAL, new BigDecimal("10000.00"), BigDecimal.ZERO),
                createTransaction(agentId, TransactionType.CASH_DEPOSIT, new BigDecimal("3000.00"), BigDecimal.ZERO),
                createTransaction(agentId, TransactionType.JOMPAY, new BigDecimal("2000.00"), new BigDecimal("500.00"))
            ));

        when(settlementSummaryRepository.save(any(SettlementSummaryRecord.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SettlementSummaryRecord result = settlementService.calculateNetSettlement(agentId, settlementDate);

        assertNotNull(result);
        assertEquals(agentId, result.agentId());
        assertEquals(settlementDate, result.settlementDate());
        assertEquals(new BigDecimal("10000.00"), result.totalWithdrawals());
        assertEquals(new BigDecimal("3000.00"), result.totalDeposits());
        assertEquals(new BigDecimal("2000.00"), result.totalBillPayments());
        assertEquals(new BigDecimal("500.00"), result.totalCommissions());
        assertEquals(new BigDecimal("5500.00"), result.netAmount());
        assertEquals(SettlementDirection.BANK_OWES_AGENT, result.direction());
        assertEquals("MYR", result.currency());

        verify(settlementSummaryRepository).save(any(SettlementSummaryRecord.class));
    }

    @Test
    void shouldCalculateNegativeNetSettlement_agentOwesBank() {
        when(transactionRepository.findByAgentIdAndCompletedDate(agentId, settlementDate))
            .thenReturn(List.of(
                createTransaction(agentId, TransactionType.CASH_WITHDRAWAL, new BigDecimal("3000.00"), BigDecimal.ZERO),
                createTransaction(agentId, TransactionType.CASH_DEPOSIT, new BigDecimal("10000.00"), BigDecimal.ZERO),
                createTransaction(agentId, TransactionType.JOMPAY, new BigDecimal("2000.00"), new BigDecimal("500.00"))
            ));

        when(settlementSummaryRepository.save(any(SettlementSummaryRecord.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SettlementSummaryRecord result = settlementService.calculateNetSettlement(agentId, settlementDate);

        assertEquals(new BigDecimal("3000.00"), result.totalWithdrawals());
        assertEquals(new BigDecimal("10000.00"), result.totalDeposits());
        assertEquals(new BigDecimal("2000.00"), result.totalBillPayments());
        assertEquals(new BigDecimal("500.00"), result.totalCommissions());
        assertEquals(new BigDecimal("-8500.00"), result.netAmount());
        assertEquals(SettlementDirection.AGENT_OWES_BANK, result.direction());
    }

    @Test
    void shouldIncludeRetailSalesInNetCalculation() {
        when(transactionRepository.findByAgentIdAndCompletedDate(agentId, settlementDate))
            .thenReturn(List.of(
                createTransaction(agentId, TransactionType.CASH_WITHDRAWAL, new BigDecimal("5000.00"), BigDecimal.ZERO),
                createTransaction(agentId, TransactionType.CASH_DEPOSIT, new BigDecimal("2000.00"), BigDecimal.ZERO),
                createTransaction(agentId, TransactionType.ESSP_PURCHASE, new BigDecimal("3000.00"), new BigDecimal("200.00")),
                createTransaction(agentId, TransactionType.ASTRO_RPN, new BigDecimal("1000.00"), BigDecimal.ZERO)
            ));

        when(settlementSummaryRepository.save(any(SettlementSummaryRecord.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SettlementSummaryRecord result = settlementService.calculateNetSettlement(agentId, settlementDate);

        assertEquals(new BigDecimal("3000.00"), result.totalRetailSales());
        assertEquals(new BigDecimal("5200.00"), result.netAmount());
        assertEquals(SettlementDirection.BANK_OWES_AGENT, result.direction());
    }

    @Test
    void shouldGenerateCsvForCBS() {
        SettlementSummaryRecord record1 = new SettlementSummaryRecord(
            UUID.randomUUID(), UUID.randomUUID(), settlementDate,
            new BigDecimal("10000.00"), new BigDecimal("3000.00"),
            new BigDecimal("2000.00"), BigDecimal.ZERO, new BigDecimal("500.00"),
            new BigDecimal("5500.00"), SettlementDirection.BANK_OWES_AGENT,
            "MYR", LocalDateTime.now()
        );
        SettlementSummaryRecord record2 = new SettlementSummaryRecord(
            UUID.randomUUID(), UUID.randomUUID(), settlementDate,
            new BigDecimal("3000.00"), new BigDecimal("10000.00"),
            new BigDecimal("2000.00"), BigDecimal.ZERO, new BigDecimal("500.00"),
            new BigDecimal("-8500.00"), SettlementDirection.AGENT_OWES_BANK,
            "MYR", LocalDateTime.now()
        );

        String expectedCsv = "settlement_id,agent_id,settlement_date,total_withdrawals,total_deposits,total_bill_payments,total_retail_sales,total_commissions,net_amount,direction,currency\n";
        when(cbsFileGenerator.generateCsv(anyList())).thenReturn(expectedCsv);

        String csv = settlementService.generateCbsFile(List.of(record1, record2));

        assertNotNull(csv);
        assertTrue(csv.contains("settlement_id"));
        assertTrue(csv.contains("agent_id"));
        assertTrue(csv.contains("net_amount"));
        assertTrue(csv.contains("direction"));
        verify(cbsFileGenerator).generateCsv(argThat(list -> list.size() == 2));
    }

    @Test
    void shouldRunEodSettlementForAllAgentsWithActivity() {
        UUID agent2Id = UUID.randomUUID();
        when(transactionRepository.findAgentIdsWithTransactionsOnDate(settlementDate))
            .thenReturn(List.of(agentId, agent2Id));
        when(transactionRepository.findByAgentIdAndCompletedDate(eq(agentId), eq(settlementDate)))
            .thenReturn(List.of(
                createTransaction(agentId, TransactionType.CASH_WITHDRAWAL, new BigDecimal("5000.00"), BigDecimal.ZERO)
            ));
        when(transactionRepository.findByAgentIdAndCompletedDate(eq(agent2Id), eq(settlementDate)))
            .thenReturn(List.of(
                createTransaction(agent2Id, TransactionType.CASH_DEPOSIT, new BigDecimal("3000.00"), BigDecimal.ZERO)
            ));
        when(settlementSummaryRepository.save(any(SettlementSummaryRecord.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(cbsFileGenerator.generateCsv(anyList())).thenReturn("csv-content");

        List<SettlementSummaryRecord> results = settlementService.runEodSettlement(settlementDate);

        assertEquals(2, results.size());
        verify(settlementSummaryRepository, times(2)).save(any(SettlementSummaryRecord.class));
        verify(cbsFileGenerator).generateCsv(argThat(list -> list.size() == 2));
    }

    @Test
    void shouldHandleAgentWithNoTransactions_gracefully() {
        when(transactionRepository.findAgentIdsWithTransactionsOnDate(settlementDate))
            .thenReturn(List.of());

        List<SettlementSummaryRecord> results = settlementService.runEodSettlement(settlementDate);

        assertTrue(results.isEmpty());
        verify(settlementSummaryRepository, never()).save(any());
    }

    @Test
    void shouldReturnZeroNetWhenAllCategoriesZero() {
        when(transactionRepository.findByAgentIdAndCompletedDate(agentId, settlementDate))
            .thenReturn(List.of());
        when(settlementSummaryRepository.save(any(SettlementSummaryRecord.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        SettlementSummaryRecord result = settlementService.calculateNetSettlement(agentId, settlementDate);

        assertEquals(BigDecimal.ZERO.compareTo(result.netAmount()), 0);
    }

    private TransactionRecord createTransaction(UUID agentId, TransactionType type,
                                                  BigDecimal amount, BigDecimal commission) {
        return new TransactionRecord(
            UUID.randomUUID(),
            "idem-" + UUID.randomUUID(),
            agentId,
            type,
            amount,
            BigDecimal.ZERO,
            commission,
            BigDecimal.ZERO,
            TransactionStatus.COMPLETED,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}

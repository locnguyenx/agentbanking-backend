package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.SettlementDirection;
import com.agentbanking.ledger.domain.model.SettlementSummaryRecord;
import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.model.TransactionType;
import com.agentbanking.ledger.domain.port.out.CbsFileGenerator;
import com.agentbanking.ledger.domain.port.out.SettlementSummaryRepository;
import com.agentbanking.ledger.domain.port.out.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class SettlementService {

    private static final String MYR_CURRENCY = "MYR";
    private static final List<TransactionType> WITHDRAWAL_TYPES = List.of(
        TransactionType.CASH_WITHDRAWAL,
        TransactionType.SARAWAK_PAY_WITHDRAWAL
    );
    private static final List<TransactionType> DEPOSIT_TYPES = List.of(
        TransactionType.CASH_DEPOSIT,
        TransactionType.SARAWAK_PAY_TOPUP
    );
    private static final List<TransactionType> BILL_PAYMENT_TYPES = List.of(
        TransactionType.JOMPAY,
        TransactionType.ASTRO_RPN,
        TransactionType.TM_RPN,
        TransactionType.EPF_PAYMENT,
        TransactionType.CELCOM_TOPUP,
        TransactionType.M1_TOPUP
    );
    private static final List<TransactionType> RETAIL_SALE_TYPES = List.of(
        TransactionType.ESSP_PURCHASE,
        TransactionType.PIN_PURCHASE,
        TransactionType.CASHLESS_PAYMENT
    );

    private final TransactionRepository transactionRepository;
    private final SettlementSummaryRepository settlementSummaryRepository;
    private final CbsFileGenerator cbsFileGenerator;

    public SettlementService(
            TransactionRepository transactionRepository,
            SettlementSummaryRepository settlementSummaryRepository,
            CbsFileGenerator cbsFileGenerator) {
        this.transactionRepository = transactionRepository;
        this.settlementSummaryRepository = settlementSummaryRepository;
        this.cbsFileGenerator = cbsFileGenerator;
    }

    public SettlementSummaryRecord calculateNetSettlement(UUID agentId, LocalDate date) {
        List<TransactionRecord> transactions = transactionRepository.findByAgentIdAndCompletedDate(agentId, date);

        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalBillPayments = BigDecimal.ZERO;
        BigDecimal totalRetailSales = BigDecimal.ZERO;
        BigDecimal totalCommissions = BigDecimal.ZERO;

        for (TransactionRecord txn : transactions) {
            if (WITHDRAWAL_TYPES.contains(txn.transactionType())) {
                totalWithdrawals = totalWithdrawals.add(txn.amount());
            }
            if (DEPOSIT_TYPES.contains(txn.transactionType())) {
                totalDeposits = totalDeposits.add(txn.amount());
            }
            if (BILL_PAYMENT_TYPES.contains(txn.transactionType())) {
                totalBillPayments = totalBillPayments.add(txn.amount());
            }
            if (RETAIL_SALE_TYPES.contains(txn.transactionType())) {
                totalRetailSales = totalRetailSales.add(txn.amount());
            }
            totalCommissions = totalCommissions.add(txn.agentCommission());
        }

        BigDecimal debitSide = totalWithdrawals.add(totalCommissions).add(totalRetailSales);
        BigDecimal creditSide = totalDeposits.add(totalBillPayments);
        BigDecimal netAmount = debitSide.subtract(creditSide).setScale(2, RoundingMode.HALF_UP);

        SettlementDirection direction = netAmount.compareTo(BigDecimal.ZERO) >= 0
            ? SettlementDirection.BANK_OWES_AGENT
            : SettlementDirection.AGENT_OWES_BANK;

        SettlementSummaryRecord summary = new SettlementSummaryRecord(
            UUID.randomUUID(),
            agentId,
            date,
            totalWithdrawals.setScale(2, RoundingMode.HALF_UP),
            totalDeposits.setScale(2, RoundingMode.HALF_UP),
            totalBillPayments.setScale(2, RoundingMode.HALF_UP),
            totalRetailSales.setScale(2, RoundingMode.HALF_UP),
            totalCommissions.setScale(2, RoundingMode.HALF_UP),
            netAmount,
            direction,
            MYR_CURRENCY,
            LocalDateTime.now()
        );

        settlementSummaryRepository.save(summary);
        return summary;
    }

    public List<SettlementSummaryRecord> runEodSettlement(LocalDate date) {
        List<UUID> agentIds = transactionRepository.findAgentIdsWithTransactionsOnDate(date);

        List<SettlementSummaryRecord> settlements = agentIds.stream()
            .map(agentId -> calculateNetSettlement(agentId, date))
            .toList();

        if (!settlements.isEmpty()) {
            cbsFileGenerator.generateCsv(settlements);
        }

        return settlements;
    }

    public String generateCbsFile(List<SettlementSummaryRecord> settlements) {
        return cbsFileGenerator.generateCsv(settlements);
    }
}

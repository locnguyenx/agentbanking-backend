package com.agentbanking.ledger.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record SettlementSummaryRecord(
    UUID settlementId,
    UUID agentId,
    LocalDate settlementDate,
    BigDecimal totalWithdrawals,
    BigDecimal totalDeposits,
    BigDecimal totalBillPayments,
    BigDecimal totalRetailSales,
    BigDecimal totalCommissions,
    BigDecimal netAmount,
    SettlementDirection direction,
    String currency,
    LocalDateTime generatedAt
) {}

package com.agentbanking.ledger.infrastructure.persistence.entity;

import com.agentbanking.ledger.domain.model.SettlementDirection;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_summary")
public class SettlementSummaryEntity {

    @Id
    @Column(name = "settlement_id")
    private UUID settlementId;

    @Column(name = "agent_id")
    private UUID agentId;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

    @Column(name = "total_withdrawals")
    private BigDecimal totalWithdrawals;

    @Column(name = "total_deposits")
    private BigDecimal totalDeposits;

    @Column(name = "total_bill_payments")
    private BigDecimal totalBillPayments;

    @Column(name = "total_retail_sales")
    private BigDecimal totalRetailSales;

    @Column(name = "total_commissions")
    private BigDecimal totalCommissions;

    @Column(name = "net_amount")
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction")
    private SettlementDirection direction;

    @Column(name = "currency")
    private String currency;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    public UUID getSettlementId() { return settlementId; }
    public void setSettlementId(UUID settlementId) { this.settlementId = settlementId; }
    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }
    public BigDecimal getTotalWithdrawals() { return totalWithdrawals; }
    public void setTotalWithdrawals(BigDecimal totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; }
    public BigDecimal getTotalDeposits() { return totalDeposits; }
    public void setTotalDeposits(BigDecimal totalDeposits) { this.totalDeposits = totalDeposits; }
    public BigDecimal getTotalBillPayments() { return totalBillPayments; }
    public void setTotalBillPayments(BigDecimal totalBillPayments) { this.totalBillPayments = totalBillPayments; }
    public BigDecimal getTotalRetailSales() { return totalRetailSales; }
    public void setTotalRetailSales(BigDecimal totalRetailSales) { this.totalRetailSales = totalRetailSales; }
    public BigDecimal getTotalCommissions() { return totalCommissions; }
    public void setTotalCommissions(BigDecimal totalCommissions) { this.totalCommissions = totalCommissions; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public SettlementDirection getDirection() { return direction; }
    public void setDirection(SettlementDirection direction) { this.direction = direction; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}

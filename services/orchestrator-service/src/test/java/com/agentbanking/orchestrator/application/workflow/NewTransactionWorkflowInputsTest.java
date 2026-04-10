package com.agentbanking.orchestrator.application.workflow;

import com.agentbanking.orchestrator.application.workflow.CashlessPaymentWorkflow.CashlessPaymentInput;
import com.agentbanking.orchestrator.application.workflow.PrepaidTopupWorkflow.PrepaidTopupInput;
import com.agentbanking.orchestrator.application.workflow.EWalletWithdrawalWorkflow.EWalletWithdrawalInput;
import com.agentbanking.orchestrator.application.workflow.EWalletTopupWorkflow.EWalletTopupInput;
import com.agentbanking.orchestrator.application.workflow.ESSPPurchaseWorkflow.ESSPPurchaseInput;
import com.agentbanking.orchestrator.application.workflow.PINPurchaseWorkflow.PINPurchaseInput;
import com.agentbanking.orchestrator.application.workflow.PinBasedPurchaseWorkflow.PinBasedPurchaseInput;
import com.agentbanking.orchestrator.application.workflow.RetailSaleWorkflow.RetailSaleInput;
import com.agentbanking.orchestrator.application.workflow.HybridCashbackWorkflow.HybridCashbackInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NewTransactionWorkflowInputsTest {

    @Test
    void shouldCreateCashlessPaymentInput() {
        var input = new CashlessPaymentInput(
            UUID.randomUUID(),
            "QR",
            new BigDecimal("100.00"),
            "idem-123",
            "60191234567",
            new BigDecimal("3.0"),
            new BigDecimal("101.0"),
            "TIER_1",
            "target-bin-123"
        );
        
        assertNotNull(input.agentId());
        assertEquals("QR", input.paymentMethod());
        assertEquals(new BigDecimal("100.00"), input.amount());
    }

    @Test
    void shouldCreatePrepaidTopupInput() {
        var input = new PrepaidTopupInput(
            UUID.randomUUID(),
            "CELCOM",
            "60191234567",
            new BigDecimal("50.00"),
            "idem-124",
            new BigDecimal("3.0"),
            new BigDecimal("101.0"),
            "TIER_1",
            "target-bin-123"
        );
        
        assertNotNull(input.agentId());
        assertEquals("CELCOM", input.telcoProvider());
        assertEquals("60191234567", input.phoneNumber());
    }

    @Test
    void shouldCreateEWalletWithdrawalInput() {
        var input = new EWalletWithdrawalInput(
            UUID.randomUUID(),
            "TOUCHNGO",
            "wallet-123",
            new BigDecimal("200.00"),
            "idem-125",
            new BigDecimal("3.0"),
            new BigDecimal("101.0"),
            "TIER_1",
            "target-bin-123"
        );
        
        assertNotNull(input.agentId());
        assertEquals("TOUCHNGO", input.provider());
        assertEquals("wallet-123", input.walletId());
    }

    @Test
    void shouldCreateEWalletTopupInput() {
        var input = new EWalletTopupInput(
            UUID.randomUUID(),
            "TOUCHNGO",
            "wallet-123",
            new BigDecimal("100.00"),
            "idem-126",
            new BigDecimal("3.0"),
            new BigDecimal("101.0"),
            "TIER_1",
            "target-bin-123"
        );
        
        assertNotNull(input.agentId());
        assertEquals("TOUCHNGO", input.provider());
    }

    @Test
    void shouldCreateESSPPurchaseInput() {
        var input = new ESSPPurchaseInput(
            UUID.randomUUID(),
            new BigDecimal("1000.00"),
            "123456789012",
            "idem-127",
            new BigDecimal("3.0"),
            new BigDecimal("101.0"),
            "TIER_1",
            "target-bin-123"
        );
        
        assertNotNull(input.agentId());
        assertEquals(new BigDecimal("1000.00"), input.amount());
        assertEquals("123456789012", input.customerMykad());
    }

    @Test
    void shouldCreatePINPurchaseInput() {
        var input = new PINPurchaseInput(
            UUID.randomUUID(),
            "GLOBE",
            new BigDecimal("100.00"),
            5,
            "idem-128",
            new BigDecimal("3.0"),
            new BigDecimal("101.0"),
            "TIER_1",
            "target-bin-123"
        );
        
        assertNotNull(input.agentId());
        assertEquals("GLOBE", input.provider());
        assertEquals(new BigDecimal("100.00"), input.faceValue());
        assertEquals(5, input.quantity());
    }

    @Test
    void shouldCreatePinBasedPurchaseInput() {
        var input = new PinBasedPurchaseInput(
            UUID.randomUUID(),
            "MAXIS",
            new BigDecimal("50.00"),
            2,
            "123456789012",
            "idem-129",
            new BigDecimal("3.0"),
            new BigDecimal("101.0"),
            "TIER_1",
            "target-bin-123"
        );
        
        assertNotNull(input.agentId());
        assertEquals("MAXIS", input.provider());
        assertEquals(2, input.quantity());
    }

    @Test
    void shouldCreateRetailSaleInput() {
        var input = new RetailSaleInput(
            UUID.randomUUID(),
            "QR",
            new BigDecimal("500.00"),
            "RETAIL",
            "idem-130",
            "60191234567",
            new BigDecimal("3.0"),
            new BigDecimal("101.0"),
            "TIER_1",
            "target-bin-123"
        );
        
        assertNotNull(input.agentId());
        assertEquals("QR", input.paymentMethod());
        assertEquals(new BigDecimal("500.00"), input.amount());
        assertEquals("RETAIL", input.merchantType());
    }

    @Test
    void shouldCreateHybridCashbackInput() {
        var input = new HybridCashbackInput(
            UUID.randomUUID(),
            "QR",
            new BigDecimal("500.00"),
            new BigDecimal("50.00"),
            "idem-131",
            "60191234567",
            new BigDecimal("3.0"),
            new BigDecimal("101.0"),
            "TIER_1",
            "target-bin-123"
        );
        
        assertNotNull(input.agentId());
        assertEquals("QR", input.paymentMethod());
        assertEquals(new BigDecimal("500.00"), input.purchaseAmount());
        assertEquals(new BigDecimal("50.00"), input.cashbackAmount());
    }
}

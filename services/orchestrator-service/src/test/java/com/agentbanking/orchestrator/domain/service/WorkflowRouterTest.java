package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkflowRouterTest {

    private final WorkflowRouter router = new WorkflowRouter();

    @ParameterizedTest
    @EnumSource(value = TransactionType.class, names = {
        "CASHLESS_PAYMENT", "PIN_BASED_PURCHASE", "PREPAID_TOPUP",
        "EWALLET_WITHDRAWAL", "EWALLET_TOPUP", "ESSP_PURCHASE",
        "PIN_PURCHASE", "RETAIL_SALE", "HYBRID_CASHBACK"
    })
    void shouldRouteNewTransactionTypes(TransactionType type) {
        String workflow = router.determineWorkflowType(type, null);
        assertEquals(type.name(), workflow);
    }

    @Test
    void shouldRouteCashlessPayment() {
        assertEquals("CASHLESS_PAYMENT", router.determineWorkflowType(TransactionType.CASHLESS_PAYMENT, null));
    }

    @Test
    void shouldRoutePinBasedPurchase() {
        assertEquals("PIN_BASED_PURCHASE", router.determineWorkflowType(TransactionType.PIN_BASED_PURCHASE, null));
    }

    @Test
    void shouldRoutePrepaidTopup() {
        assertEquals("PREPAID_TOPUP", router.determineWorkflowType(TransactionType.PREPAID_TOPUP, null));
    }

    @Test
    void shouldRouteEWalletWithdrawal() {
        assertEquals("EWALLET_WITHDRAWAL", router.determineWorkflowType(TransactionType.EWALLET_WITHDRAWAL, null));
    }

    @Test
    void shouldRouteEWalletTopup() {
        assertEquals("EWALLET_TOPUP", router.determineWorkflowType(TransactionType.EWALLET_TOPUP, null));
    }

    @Test
    void shouldRouteESSPPurchase() {
        assertEquals("ESSP_PURCHASE", router.determineWorkflowType(TransactionType.ESSP_PURCHASE, null));
    }

    @Test
    void shouldRoutePINPurchase() {
        assertEquals("PIN_PURCHASE", router.determineWorkflowType(TransactionType.PIN_PURCHASE, null));
    }

    @Test
    void shouldRouteRetailSale() {
        assertEquals("RETAIL_SALE", router.determineWorkflowType(TransactionType.RETAIL_SALE, null));
    }

    @Test
    void shouldRouteHybridCashback() {
        assertEquals("HYBRID_CASHBACK", router.determineWorkflowType(TransactionType.HYBRID_CASHBACK, null));
    }

    @Test
    void shouldRouteCashWithdrawalOnUs() {
        assertEquals("WithdrawalOnUs", router.determineWorkflowType(TransactionType.CASH_WITHDRAWAL, "0012"));
    }

    @Test
    void shouldRouteCashWithdrawalOffUs() {
        assertEquals("Withdrawal", router.determineWorkflowType(TransactionType.CASH_WITHDRAWAL, "9999"));
    }
}

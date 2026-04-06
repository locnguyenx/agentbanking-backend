package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.model.TransactionType;

public class WorkflowRouter {

    private static final String BSN_BIN = "0012";

    public String determineWorkflowType(TransactionType transactionType, String targetBIN) {
        return switch (transactionType) {
            case CASH_WITHDRAWAL -> isOnUs(targetBIN) ? "WithdrawalOnUs" : "Withdrawal";
            case CASH_DEPOSIT -> "Deposit";
            case BILL_PAYMENT -> "BillPayment";
            case DUITNOW_TRANSFER -> "DuitNowTransfer";
            case CASHLESS_PAYMENT -> "CASHLESS_PAYMENT";
            case PIN_BASED_PURCHASE -> "PIN_BASED_PURCHASE";
            case PREPAID_TOPUP -> "PREPAID_TOPUP";
            case EWALLET_WITHDRAWAL -> "EWALLET_WITHDRAWAL";
            case EWALLET_TOPUP -> "EWALLET_TOPUP";
            case ESSP_PURCHASE -> "ESSP_PURCHASE";
            case PIN_PURCHASE -> "PIN_PURCHASE";
            case RETAIL_SALE -> "RETAIL_SALE";
            case HYBRID_CASHBACK -> "HYBRID_CASHBACK";
        };
    }

    private boolean isOnUs(String targetBIN) {
        return BSN_BIN.equals(targetBIN);
    }
}
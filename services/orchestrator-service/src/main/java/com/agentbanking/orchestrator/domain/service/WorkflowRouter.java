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
        };
    }

    private boolean isOnUs(String targetBIN) {
        return BSN_BIN.equals(targetBIN);
    }
}
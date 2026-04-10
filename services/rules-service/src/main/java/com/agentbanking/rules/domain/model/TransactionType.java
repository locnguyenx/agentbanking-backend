package com.agentbanking.rules.domain.model;

public enum TransactionType {
    CASH_WITHDRAWAL,
    CASH_DEPOSIT,
    BALANCE_INQUIRY,
    DUITNOW_TRANSFER,
    BILL_PAYMENT,
    JOMPAY,
    ASTRO_RPN,
    TM_RPN,
    EPF_PAYMENT,
    PREPAID_TOPUP,
    CELCOM_TOPUP,
    M1_TOPUP,
    SARAWAK_PAY_WITHDRAWAL,
    SARAWAK_PAY_TOPUP,
    ESSP_PURCHASE,
    PIN_PURCHASE,
    CASHLESS_PAYMENT,
    PIN_BASED_PURCHASE,
    EWALLET_WITHDRAWAL,
    EWALLET_TOPUP,
    RETAIL_SALE,
    HYBRID_CASHBACK;

    public static TransactionType fromFrontend(String serviceCode) {
        if (serviceCode == null) {
            throw new IllegalArgumentException("serviceCode cannot be null");
        }
        try {
            return valueOf(serviceCode);
        } catch (IllegalArgumentException e) {
            return switch (serviceCode) {
                case "JOMPAY", "ASTRO_RPN", "TM_RPN" -> BILL_PAYMENT;
                case "CELCOM_TOPUP", "M1_TOPUP" -> PREPAID_TOPUP;
                default -> throw new IllegalArgumentException("Unknown transaction type: " + serviceCode);
            };
        }
    }
}
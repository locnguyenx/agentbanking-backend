package com.agentbanking.common.security;

/**
 * Centralized error code registry for the Agent Banking Platform.
 * All error codes must come from this registry.
 * 
 * Categories:
 * - ERR_VAL_xxx: Validation errors
 * - ERR_BIZ_xxx: Business logic errors
 * - ERR_EXT_xxx: External system errors
 * - ERR_AUTH_xxx: Authentication errors
 * - ERR_SYS_xxx: System errors
 */
public final class ErrorCodes {

    private ErrorCodes() {}

    // Validation Errors (ERR_VAL_xxx)
    public static final String ERR_INVALID_AMOUNT = "ERR_VAL_INVALID_AMOUNT";
    public static final String ERR_INVALID_MYKAD_FORMAT = "ERR_VAL_INVALID_MYKAD_FORMAT";
    public static final String ERR_INVALID_PHONE_NUMBER = "ERR_VAL_INVALID_PHONE_NUMBER";
    public static final String ERR_INVALID_BILLER_REF = "ERR_VAL_INVALID_BILLER_REF";
    public static final String ERR_MISSING_VERIFICATION_ID = "ERR_VAL_MISSING_VERIFICATION_ID";

    // Business Errors (ERR_BIZ_xxx)
    public static final String ERR_INSUFFICIENT_FLOAT = "ERR_BIZ_INSUFFICIENT_FLOAT";
    public static final String ERR_LIMIT_EXCEEDED = "ERR_BIZ_LIMIT_EXCEEDED";
    public static final String ERR_COUNT_LIMIT_EXCEEDED = "ERR_BIZ_COUNT_LIMIT_EXCEEDED";
    public static final String ERR_VELOCITY_COUNT_EXCEEDED = "ERR_BIZ_VELOCITY_COUNT_EXCEEDED";
    public static final String ERR_VELOCITY_AMOUNT_EXCEEDED = "ERR_BIZ_VELOCITY_AMOUNT_EXCEEDED";
    public static final String ERR_FLOAT_CAP_EXCEEDED = "ERR_BIZ_FLOAT_CAP_EXCEEDED";
    public static final String ERR_GEOFENCE_VIOLATION = "ERR_BIZ_GEOFENCE_VIOLATION";
    public static final String ERR_GPS_UNAVAILABLE = "ERR_BIZ_GPS_UNAVAILABLE";
    public static final String ERR_AGENT_DEACTIVATED = "ERR_BIZ_AGENT_DEACTIVATED";
    public static final String ERR_FEE_CONFIG_NOT_FOUND = "ERR_BIZ_FEE_CONFIG_NOT_FOUND";
    public static final String ERR_FEE_CONFIG_EXPIRED = "ERR_BIZ_FEE_CONFIG_EXPIRED";
    public static final String ERR_FEE_COMPONENTS_MISMATCH = "ERR_BIZ_FEE_COMPONENTS_MISMATCH";
    public static final String ERR_AGENT_HAS_PENDING_TRANSACTIONS = "ERR_BIZ_AGENT_HAS_PENDING_TRANSACTIONS";
    public static final String ERR_DUPLICATE_AGENT = "ERR_BIZ_DUPLICATE_AGENT";
    public static final String ERR_AGENT_NOT_FOUND = "ERR_BIZ_AGENT_NOT_FOUND";
    public static final String ERR_BIZ_QUOTE_CALCULATION_FAILED = "ERR_BIZ_QUOTE_CALCULATION_FAILED";
    public static final String ERR_BIZ_PROXY_NOT_FOUND = "ERR_BIZ_PROXY_NOT_FOUND";
    public static final String ERR_BIZ_COMPLIANCE_CHECK_FAILED = "ERR_BIZ_COMPLIANCE_CHECK_FAILED";
    public static final String ERR_UNSUPPORTED_TRANSACTION_TYPE = "ERR_BIZ_UNSUPPORTED_TRANSACTION_TYPE";
    public static final String ERR_INVALID_ACCOUNT = "ERR_BIZ_INVALID_ACCOUNT";
    public static final String ERR_BIOMETRIC_MISMATCH = "ERR_BIZ_BIOMETRIC_MISMATCH";
    public static final String ERR_BIZ_MYKAD_NOT_FOUND = "ERR_BIZ_MYKAD_NOT_FOUND";
    public static final String ERR_ESSP_SERVICE_UNAVAILABLE = "ERR_BIZ_ESSP_SERVICE_UNAVAILABLE";
    public static final String ERR_QR_PAYMENT_TIMEOUT = "ERR_BIZ_QR_PAYMENT_TIMEOUT";
    public static final String ERR_RTP_DECLINED = "ERR_BIZ_RTP_DECLINED";
    public static final String ERR_PIN_INVENTORY_DEPLETED = "ERR_BIZ_PIN_INVENTORY_DEPLETED";
    public static final String ERR_PIN_GENERATION_FAILED = "ERR_BIZ_PIN_GENERATION_FAILED";
    public static final String ERR_WALLET_INSUFFICIENT = "ERR_BIZ_WALLET_INSUFFICIENT";
    public static final String ERR_AGGREGATOR_TIMEOUT = "ERR_BIZ_AGGREGATOR_TIMEOUT";

    // External System Errors (ERR_EXT_xxx)
    public static final String ERR_SWITCH_DECLINED = "ERR_EXT_SWITCH_DECLINED";
    public static final String ERR_SWITCH_UNAVAILABLE = "ERR_EXT_SWITCH_UNAVAILABLE";
    public static final String ERR_KYC_SERVICE_UNAVAILABLE = "ERR_EXT_KYC_SERVICE_UNAVAILABLE";
    public static final String ERR_MYKAD_NOT_FOUND = "ERR_EXT_MYKAD_NOT_FOUND";
    public static final String ERR_BILLER_UNAVAILABLE = "ERR_EXT_BILLER_UNAVAILABLE";
    public static final String ERR_BIOMETRIC_SCANNER_UNAVAILABLE = "ERR_EXT_BIOMETRIC_SCANNER_UNAVAILABLE";
    public static final String ERR_SWITCH_AUTH_FAILED = "ERR_EXT_SWITCH_AUTH_FAILED";
    public static final String ERR_SWITCH_REVERSAL_FAILED = "ERR_EXT_SWITCH_REVERSAL_FAILED";
    public static final String ERR_DUITNOW_FAILED = "ERR_EXT_DUITNOW_FAILED";
    public static final String ERR_BILLER_PAYMENT_FAILED = "ERR_EXT_BILLER_PAYMENT_FAILED";
    public static final String ERR_TOPUP_FAILED = "ERR_EXT_TOPUP_FAILED";
    public static final String ERR_EXT_PROXY_ENQUIRY_FAILED = "ERR_EXT_PROXY_ENQUIRY_FAILED";
    public static final String ERR_NETWORK_TIMEOUT = "ERR_EXT_NETWORK_TIMEOUT";
    public static final String ERR_BILLER_TIMEOUT = "ERR_EXT_BILLER_TIMEOUT";

    // Auth Errors (ERR_AUTH_xxx)
    public static final String ERR_TOKEN_EXPIRED = "ERR_AUTH_TOKEN_EXPIRED";
    public static final String ERR_MISSING_TOKEN = "ERR_AUTH_MISSING_TOKEN";
    public static final String ERR_INVALID_TOKEN = "ERR_AUTH_INVALID_TOKEN";
    public static final String ERR_INVALID_PIN = "ERR_AUTH_INVALID_PIN";
    public static final String ERR_INVALID_CARD = "ERR_AUTH_INVALID_CARD";

    // System Errors (ERR_SYS_xxx)
    public static final String ERR_SERVICE_UNAVAILABLE = "ERR_SYS_SERVICE_UNAVAILABLE";
    public static final String ERR_INTERNAL = "ERR_SYS_INTERNAL";
    public static final String ERR_AGENT_FLOAT_NOT_FOUND = "ERR_SYS_AGENT_FLOAT_NOT_FOUND";

    // Currency Errors
    public static final String ERR_INVALID_CURRENCY = "ERR_VAL_INVALID_CURRENCY";
}

package com.agentbanking.orchestrator.domain.model.exceptions;

public class TelcoException extends RuntimeException {
    private final String errorCode;

    public TelcoException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class InvalidPhoneNumberException extends TelcoException {
        public InvalidPhoneNumberException(String phoneNumber) {
            super("Invalid phone number: " + phoneNumber, "ERR_INVALID_PHONE_NUMBER");
        }
    }

    public static class TelcoTopupFailedException extends TelcoException {
        public TelcoTopupFailedException(String message) {
            super(message, "ERR_TELCO_TOPUP_FAILED");
        }
    }

    public static class TelcoTimeoutException extends TelcoException {
        public TelcoTimeoutException(String provider) {
            super("Telco aggregator timeout: " + provider, "ERR_AGGREGATOR_TIMEOUT");
        }
    }

    public static class TelcoUnavailableException extends TelcoException {
        public TelcoUnavailableException(String provider) {
            super("Telco aggregator unavailable: " + provider, "ERR_TELCO_UNAVAILABLE");
        }
    }
}

package com.agentbanking.orchestrator.domain.model.exceptions;

public class EWalletException extends RuntimeException {
    private final String errorCode;

    public EWalletException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class InvalidEWalletException extends EWalletException {
        public InvalidEWalletException(String walletId) {
            super("Invalid eWallet ID: " + walletId, "ERR_INVALID_EWALLET");
        }
    }

    public static class EWalletInsufficientException extends EWalletException {
        public EWalletInsufficientException() {
            super("Insufficient eWallet balance", "ERR_WALLET_INSUFFICIENT");
        }
    }

    public static class EWalletWithdrawFailedException extends EWalletException {
        public EWalletWithdrawFailedException(String message) {
            super(message, "ERR_EWALLET_WITHDRAW_FAILED");
        }
    }

    public static class EWalletTopupFailedException extends EWalletException {
        public EWalletTopupFailedException(String message) {
            super(message, "ERR_EWALLET_TOPUP_FAILED");
        }
    }

    public static class EWalletTimeoutException extends EWalletException {
        public EWalletTimeoutException(String provider) {
            super("eWallet provider timeout: " + provider, "ERR_EWALLET_TIMEOUT");
        }
    }

    public static class EWalletUnavailableException extends EWalletException {
        public EWalletUnavailableException(String provider) {
            super("eWallet provider unavailable: " + provider, "ERR_EWALLET_UNAVAILABLE");
        }
    }
}

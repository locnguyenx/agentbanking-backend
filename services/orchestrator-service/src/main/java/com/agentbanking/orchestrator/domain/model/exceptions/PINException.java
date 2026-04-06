package com.agentbanking.orchestrator.domain.model.exceptions;

public class PINException extends RuntimeException {
    private final String errorCode;

    public PINException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class PINInventoryDepletedException extends PINException {
        public PINInventoryDepletedException(String provider) {
            super("PIN inventory depleted for provider: " + provider, "ERR_PIN_INVENTORY_DEPLETED");
        }
    }

    public static class PINProviderUnavailableException extends PINException {
        public PINProviderUnavailableException(String provider) {
            super("PIN provider unavailable: " + provider, "ERR_PIN_PROVIDER_UNAVAILABLE");
        }
    }

    public static class PINGenerationFailedException extends PINException {
        public PINGenerationFailedException(String message) {
            super(message, "ERR_PIN_GENERATION_FAILED");
        }
    }
}

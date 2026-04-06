package com.agentbanking.orchestrator.domain.model.exceptions;

public class ESSPException extends RuntimeException {
    private final String errorCode;

    public ESSPException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class ESSPInvalidAmountException extends ESSPException {
        public ESSPInvalidAmountException() {
            super("Invalid eSSP purchase amount", "ERR_ESSP_INVALID_AMOUNT");
        }
    }

    public static class ESSPServiceUnavailableException extends ESSPException {
        public ESSPServiceUnavailableException() {
            super("BSN eSSP service unavailable", "ERR_ESSP_SERVICE_UNAVAILABLE");
        }
    }

    public static class ESSPPurchaseFailedException extends ESSPException {
        public ESSPPurchaseFailedException(String message) {
            super(message, "ERR_ESSP_PURCHASE_FAILED");
        }
    }
}

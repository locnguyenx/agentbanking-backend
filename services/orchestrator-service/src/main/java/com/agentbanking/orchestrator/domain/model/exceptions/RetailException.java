package com.agentbanking.orchestrator.domain.model.exceptions;

public class RetailException extends RuntimeException {
    private final String errorCode;

    public RetailException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static class MDRConfigNotFoundException extends RetailException {
        public MDRConfigNotFoundException(String transactionType, String paymentMethod) {
            super("MDR config not found for " + transactionType + " / " + paymentMethod, "ERR_MDR_CONFIG_NOT_FOUND");
        }
    }

    public static class QRGenerationFailedException extends RetailException {
        public QRGenerationFailedException(String message) {
            super(message, "ERR_QR_GENERATION_FAILED");
        }
    }

    public static class QRPaymentTimeoutException extends RetailException {
        public QRPaymentTimeoutException() {
            super("QR payment timeout", "ERR_QR_PAYMENT_TIMEOUT");
        }
    }

    public static class QRPaymentFailedException extends RetailException {
        public QRPaymentFailedException(String message) {
            super(message, "ERR_QR_PAYMENT_FAILED");
        }
    }

    public static class RTPSendFailedException extends RetailException {
        public RTPSendFailedException(String message) {
            super(message, "ERR_RTP_SEND_FAILED");
        }
    }

    public static class RTPInvalidProxyException extends RetailException {
        public RTPInvalidProxyException(String proxy) {
            super("Invalid RTP proxy: " + proxy, "ERR_RTP_INVALID_PROXY");
        }
    }

    public static class RTPApprovalTimeoutException extends RetailException {
        public RTPApprovalTimeoutException() {
            super("RTP approval timeout", "ERR_RTP_APPROVAL_TIMEOUT");
        }
    }

    public static class RTPDeclinedException extends RetailException {
        public RTPDeclinedException() {
            super("RTP declined by customer", "ERR_RTP_DECLINED");
        }
    }

    public static class RecordCreationFailedException extends RetailException {
        public RecordCreationFailedException(String message) {
            super(message, "ERR_RECORD_CREATION_FAILED");
        }
    }

    public static class FloatCapacityCheckFailedException extends RetailException {
        public FloatCapacityCheckFailedException() {
            super("Insufficient float capacity for cashback", "ERR_INSUFFICIENT_FLOAT");
        }
    }
}

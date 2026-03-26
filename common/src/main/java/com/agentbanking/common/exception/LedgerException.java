package com.agentbanking.common.exception;

public class LedgerException extends RuntimeException {
    private final String errorCode;
    private final String actionCode;

    public LedgerException(String errorCode, String actionCode) {
        super(errorCode);
        this.errorCode = errorCode;
        this.actionCode = actionCode;
    }

    public LedgerException(String errorCode, String actionCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.actionCode = actionCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getActionCode() {
        return actionCode;
    }
}

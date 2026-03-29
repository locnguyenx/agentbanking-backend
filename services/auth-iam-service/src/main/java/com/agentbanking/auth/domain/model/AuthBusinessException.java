package com.agentbanking.auth.domain.model;

public class AuthBusinessException extends RuntimeException {

    private final String errorCode;
    private final String actionCode;

    public AuthBusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.actionCode = "RETRY";
    }

    public AuthBusinessException(String errorCode, String message, String actionCode) {
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

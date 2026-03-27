package com.agentbanking.common.exception;

public class AgentException extends RuntimeException {

    private final String errorCode;
    private final String actionCode;

    public AgentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.actionCode = "RETRY";
    }

    public AgentException(String errorCode, String message, String actionCode) {
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
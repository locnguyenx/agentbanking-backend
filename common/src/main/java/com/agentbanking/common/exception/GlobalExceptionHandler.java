package com.agentbanking.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(LedgerException.class)
    public ResponseEntity<ErrorResponse> handleLedgerException(LedgerException e) {
        log.error("Ledger exception: {} - {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.badRequest().body(
            ErrorResponse.of(e.getErrorCode(), e.getMessage(), e.getActionCode())
        );
    }

    @ExceptionHandler(AgentException.class)
    public ResponseEntity<ErrorResponse> handleAgentException(AgentException e) {
        log.error("Agent exception: {} - {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.badRequest().body(
            ErrorResponse.of(e.getErrorCode(), e.getMessage(), e.getActionCode())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return ResponseEntity.badRequest().body(
            ErrorResponse.of("ERR_VAL_INVALID_REQUEST", e.getMessage(), "RETRY")
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.error("Illegal state: {}", e.getMessage());
        return ResponseEntity.badRequest().body(
            ErrorResponse.of("ERR_BIZ_INVALID_STATE", e.getMessage(), "RETRY")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError().body(
            ErrorResponse.of("ERR_SYS_INTERNAL", "An unexpected error occurred", "RETRY")
        );
    }
}

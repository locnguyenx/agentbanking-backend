package com.agentbanking.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("Validation failed");
        
        log.error("Validation error: {}", message);
        return ResponseEntity.badRequest().body(
            ErrorResponse.of("ERR_VAL_INVALID_REQUEST", message, "DECLINE")
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
        String message = e.getMessage() != null ? e.getMessage() : "Invalid request body";
        log.error("Message not readable: {}", message);
        return ResponseEntity.badRequest().body(
            ErrorResponse.of("ERR_VAL_INVALID_REQUEST", message, "DECLINE")
        );
    }

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
        
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        // Return 401 for authentication-related errors
        if (message.contains("credentials") || message.contains("invalid refresh token")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.of("ERR_AUTH_INVALID_CREDENTIALS", "Invalid credentials", "DECLINE")
            );
        }
        
        return ResponseEntity.badRequest().body(
            ErrorResponse.of("ERR_VAL_INVALID_REQUEST", e.getMessage(), "RETRY")
        );
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException e) {
        log.error("Security exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse.of("ERR_AUTH_UNAUTHORIZED", e.getMessage(), "DECLINE")
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

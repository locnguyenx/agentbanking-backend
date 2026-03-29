package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.domain.model.AuthBusinessException;
import com.agentbanking.common.exception.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.error("Validation error: {}", message);
        return ResponseEntity.badRequest().body(
                ErrorResponse.of("ERR_VAL_INVALID_REQUEST", message, "DECLINE", UUID.randomUUID().toString())
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("Illegal argument: {}", e.getMessage());
        return ResponseEntity.badRequest().body(
                ErrorResponse.of("ERR_VAL_INVALID_REQUEST", e.getMessage(), "RETRY", UUID.randomUUID().toString())
        );
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException e) {
        log.error("Security exception: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.of("ERR_AUTH_UNAUTHORIZED", e.getMessage(), "DECLINE", UUID.randomUUID().toString())
        );
    }

    @ExceptionHandler(AuthBusinessException.class)
    public ResponseEntity<ErrorResponse> handleAuthBusinessException(AuthBusinessException e) {
        log.error("Business exception: {} - {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.badRequest().body(
                ErrorResponse.of(e.getErrorCode(), e.getMessage(), e.getActionCode(), UUID.randomUUID().toString())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError().body(
                ErrorResponse.of("ERR_SYS_INTERNAL", "An unexpected error occurred", "RETRY", UUID.randomUUID().toString())
        );
    }
}

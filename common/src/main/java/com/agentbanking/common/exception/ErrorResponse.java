package com.agentbanking.common.exception;

import java.time.Instant;

public record ErrorResponse(
    String status,
    ErrorDetail error
) {
    public static ErrorResponse of(String code, String message, String actionCode) {
        return new ErrorResponse("FAILED", new ErrorDetail(code, message, actionCode, null, Instant.now().toString()));
    }

    public static ErrorResponse of(String code, String message, String actionCode, String traceId) {
        return new ErrorResponse("FAILED", new ErrorDetail(code, message, actionCode, traceId, Instant.now().toString()));
    }

    public record ErrorDetail(
        String code,
        String message,
        String action_code,
        String trace_id,
        String timestamp
    ) {}
}

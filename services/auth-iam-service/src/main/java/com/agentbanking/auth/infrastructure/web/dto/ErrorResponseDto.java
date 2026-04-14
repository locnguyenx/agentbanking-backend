package com.agentbanking.auth.infrastructure.web.dto;

public record ErrorResponseDto(
    String code,
    String message,
    String actionCode
) {}
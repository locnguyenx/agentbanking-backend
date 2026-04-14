package com.agentbanking.auth.infrastructure.web.dto;

public record CreateAgentUserRequestFromId(
    String agentCode,
    String phone,
    String email,
    String businessName
) {}

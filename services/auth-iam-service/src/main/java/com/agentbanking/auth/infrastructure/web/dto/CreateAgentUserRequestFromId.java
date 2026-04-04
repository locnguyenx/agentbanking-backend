package com.agentbanking.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAgentUserRequestFromId(
    @NotBlank(message = "Agent code is required")
    String agentCode,
    
    String phone,
    
    String email,
    
    @NotBlank(message = "Business name is required")
    String businessName
) {}

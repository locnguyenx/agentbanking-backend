package com.agentbanking.auth.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAgentUserRequest(
    @NotNull(message = "Agent ID is required")
    UUID agentId,
    
    @NotBlank(message = "Agent code is required")
    String agentCode,
    
    String phone,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,
    
    @NotBlank(message = "Business name is required")
    String businessName
) {}

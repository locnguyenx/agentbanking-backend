package com.agentbanking.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public class RevokeTokenRequest {

    @NotBlank(message = "Token is required")
    private String token;

    public RevokeTokenRequest() {}

    public RevokeTokenRequest(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

package com.agentbanking.auth.application.usecase;

import com.agentbanking.auth.domain.port.in.ValidateTokenUseCase;
import com.agentbanking.auth.domain.port.out.TokenProvider;

import java.util.Set;
import java.util.UUID;

/**
 * Use case implementation for token validation.
 * Delegates to TokenProvider outbound port for infrastructure concerns.
 */
public class ValidateTokenUseCaseImpl implements ValidateTokenUseCase {

    private final TokenProvider tokenProvider;

    public ValidateTokenUseCaseImpl(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public UUID validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    @Override
    public Set<String> getPermissionsFromToken(String token) {
        return tokenProvider.getPermissionsFromToken(token);
    }
}
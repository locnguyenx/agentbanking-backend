package com.agentbanking.auth.application.usecase;

import com.agentbanking.auth.domain.model.AuthenticationResult;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.port.in.AuthenticateUserUseCase;
import com.agentbanking.auth.domain.service.AuthenticationService;

/**
 * Use case implementation for user authentication
 */
public class AuthenticateUserUseCaseImpl implements AuthenticateUserUseCase {

    private final AuthenticationService authenticationService;

    public AuthenticateUserUseCaseImpl(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public AuthenticationResult authenticate(String username, String password) {
        return authenticationService.authenticate(username, password);
    }

    @Override
    public AuthenticationResult refreshToken(String refreshToken) {
        return authenticationService.refreshToken(refreshToken);
    }
}
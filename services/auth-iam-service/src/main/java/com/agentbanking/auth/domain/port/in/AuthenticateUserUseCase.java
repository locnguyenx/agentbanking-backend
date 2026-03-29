package com.agentbanking.auth.domain.port.in;

import com.agentbanking.auth.domain.model.AuthenticationResult;

/**
 * Inbound port for user authentication use case
 */
public interface AuthenticateUserUseCase {
    /**
     * Authenticate a user with username and password
     * @param username the username
     * @param password the password
     * @return authentication result containing tokens or null if failed
     */
    AuthenticationResult authenticate(String username, String password);
    
    /**
     * Refresh an access token using a refresh token
     * @param refreshToken the refresh token
     * @return new authentication result or null if invalid/expired
     */
    AuthenticationResult refreshToken(String refreshToken);
}
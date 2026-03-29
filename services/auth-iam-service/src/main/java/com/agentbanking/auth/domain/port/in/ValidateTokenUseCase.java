package com.agentbanking.auth.domain.port.in;

import java.util.Set;
import java.util.UUID;

/**
 * Inbound port for token validation use case
 */
public interface ValidateTokenUseCase {
    /**
     * Validate a JWT token and return the user ID if valid
     * @param token the JWT token
     * @return the user ID if token is valid, null otherwise
     */
    UUID validateToken(String token);
    
    /**
     * Get the permissions associated with a token
     * @param token the JWT token
     * @return set of permission keys or empty set if invalid
     */
    Set<String> getPermissionsFromToken(String token);
}
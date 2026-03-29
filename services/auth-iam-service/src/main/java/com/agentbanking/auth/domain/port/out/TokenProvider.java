package com.agentbanking.auth.domain.port.out;

import com.agentbanking.auth.domain.model.UserRecord;

import java.util.Set;
import java.util.UUID;

/**
 * Outbound port for token operations.
 * This abstracts JWT parsing/signing from the domain layer.
 */
public interface TokenProvider {
    /**
     * Generate an access token for a user
     * @param user the user to generate a token for
     * @return the access token string
     */
    String generateAccessToken(UserRecord user);

    /**
     * Generate a refresh token for a user
     * @param user the user to generate a token for
     * @return the refresh token string
     */
    String generateRefreshToken(UserRecord user);

    /**
     * Validate a token and return the associated user ID
     * @param token the token to validate
     * @return the user ID if valid
     * @throws SecurityException if token is invalid
     */
    UUID validateToken(String token);

    /**
     * Validate a refresh token and return the associated user ID
     * @param refreshToken the refresh token to validate
     * @return the user ID if valid
     * @throws SecurityException if token is invalid
     */
    UUID validateRefreshToken(String refreshToken);

    /**
     * Extract permissions from a token
     * @param token the token to extract from
     * @return set of permission keys, or empty set if invalid
     */
    Set<String> getPermissionsFromToken(String token);

    /**
     * Extract the JWT ID (jti) from a token
     * @param token the token to extract from
     * @return the JWT ID
     * @throws SecurityException if token is invalid
     */
    String extractTokenId(String token);

    /**
     * Extract the user ID from a token
     * @param token the token to extract from
     * @return the user ID
     * @throws SecurityException if token is invalid
     */
    UUID extractUserId(String token);
}
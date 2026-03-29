package com.agentbanking.auth.domain.port.out;

/**
 * Outbound port for password hashing operations.
 * This abstracts the password encoding logic from the domain layer.
 */
public interface PasswordHasher {
    /**
     * Hash a raw password
     * @param rawPassword the plain text password
     * @return the hashed password
     */
    String hash(String rawPassword);

    /**
     * Verify a raw password against a hashed password
     * @param rawPassword the plain text password
     * @param hashedPassword the hashed password to compare against
     * @return true if the password matches, false otherwise
     */
    boolean matches(String rawPassword, String hashedPassword);
}
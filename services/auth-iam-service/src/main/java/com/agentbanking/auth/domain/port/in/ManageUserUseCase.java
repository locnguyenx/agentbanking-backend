package com.agentbanking.auth.domain.port.in;

import com.agentbanking.auth.domain.model.UserRecord;

import java.util.UUID;

/**
 * Inbound port for user management use case
 */
public interface ManageUserUseCase {
    /**
     * Create a new user
     * @param userRecord the user to create
     * @return the created user
     */
    UserRecord createUser(UserRecord userRecord);
    
    /**
     * Get a user by ID
     * @param userId the user ID
     * @return the user or null if not found
     */
    UserRecord getUserById(UUID userId);
    
    /**
     * Get a user by username
     * @param username the username
     * @return the user or null if not found
     */
    UserRecord getUserByUsername(String username);
    
    /**
     * Get a user by email
     * @param email the email
     * @return the user or null if not found
     */
    UserRecord getUserByEmail(String email);
    
    /**
     * Update an existing user
     * @param userId the user ID
     * @param userRecord the updated user data
     * @return the updated user
     */
    UserRecord updateUser(UUID userId, UserRecord userRecord);
    
    /**
     * Delete a user by ID
     * @param userId the user ID
     * @return true if deleted, false if not found
     */
    boolean deleteUser(UUID userId);
    
    /**
     * Lock a user account
     * @param userId the user ID
     * @return true if locked, false if not found
     */
    boolean lockUser(UUID userId);
    
    /**
     * Unlock a user account
     * @param userId the user ID
     * @return true if unlocked, false if not found
     */
    boolean unlockUser(UUID userId);
    
    /**
     * Reset a user's password
     * @param userId the user ID
     * @param newPasswordHash the new password hash
     * @return true if reset, false if not found
     */
    boolean resetPassword(UUID userId, String newPasswordHash);
}
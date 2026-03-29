package com.agentbanking.auth.application.usecase;

import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.port.in.CheckPermissionUseCase;
import com.agentbanking.auth.domain.port.out.UserRepository;

import java.util.UUID;

/**
 * Use case implementation for checking user permissions
 */
public class CheckPermissionUseCaseImpl implements CheckPermissionUseCase {

    private final UserRepository userRepository;

    public CheckPermissionUseCaseImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean hasPermission(UUID userId, String permissionKey) {
        // Get user and check if they have the permission
        UserRecord user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // Check if user has the permission directly
        // In a full implementation, we would also check permissions through roles
        if (user.permissions() != null && user.permissions().contains(permissionKey)) {
            return true;
        }

        // For a complete implementation, we would:
        // 1. Get all roles for the user
        // 2. Get all permissions for those roles
        // 3. Check if the permissionKey is in the combined set
        
        // Placeholder implementation - always return false for now
        // This would be enhanced with proper role-permission logic
        return false;
    }
}
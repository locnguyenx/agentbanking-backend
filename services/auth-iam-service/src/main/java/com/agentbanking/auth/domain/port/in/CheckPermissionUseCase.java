package com.agentbanking.auth.domain.port.in;

import java.util.UUID;

/**
 * Inbound port for checking user permissions
 */
public interface CheckPermissionUseCase {
    /**
     * Check if a user has a specific permission
     * @param userId the user ID
     * @param permissionKey the permission key (e.g., "ledger:read")
     * @return true if the user has the permission, false otherwise
     */
    boolean hasPermission(UUID userId, String permissionKey);
}
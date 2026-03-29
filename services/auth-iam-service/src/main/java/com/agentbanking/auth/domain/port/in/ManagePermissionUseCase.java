package com.agentbanking.auth.domain.port.in;

import com.agentbanking.auth.domain.model.PermissionRecord;

import java.util.UUID;

/**
 * Inbound port for permission management use case
 */
public interface ManagePermissionUseCase {
    /**
     * Create a new permission
     * @param permissionRecord the permission to create
     * @return the created permission
     */
    PermissionRecord createPermission(PermissionRecord permissionRecord);
    
    /**
     * Get a permission by ID
     * @param permissionId the permission ID
     * @return the permission or null if not found
     */
    PermissionRecord getPermissionById(UUID permissionId);
    
    /**
     * Get a permission by key
     * @param permissionKey the permission key
     * @return the permission or null if not found
     */
    PermissionRecord getPermissionByKey(String permissionKey);
    
    /**
     * Update an existing permission
     * @param permissionId the permission ID
     * @param permissionRecord the updated permission data
     * @return the updated permission
     */
    PermissionRecord updatePermission(UUID permissionId, PermissionRecord permissionRecord);
    
    /**
     * Delete a permission by ID
     * @param permissionId the permission ID
     * @return true if deleted, false if not found
     */
    boolean deletePermission(UUID permissionId);
}
package com.agentbanking.auth.domain.port.in;

import com.agentbanking.auth.domain.model.RoleRecord;

import java.util.UUID;

/**
 * Inbound port for role management use case
 */
public interface ManageRoleUseCase {
    /**
     * Create a new role
     * @param roleRecord the role to create
     * @return the created role
     */
    RoleRecord createRole(RoleRecord roleRecord);
    
    /**
     * Get a role by ID
     * @param roleId the role ID
     * @return the role or null if not found
     */
    RoleRecord getRoleById(UUID roleId);
    
    /**
     * Get a role by name
     * @param roleName the role name
     * @return the role or null if not found
     */
    RoleRecord getRoleByName(String roleName);
    
    /**
     * Update an existing role
     * @param roleId the role ID
     * @param roleRecord the updated role data
     * @return the updated role
     */
    RoleRecord updateRole(UUID roleId, RoleRecord roleRecord);
    
    /**
     * Delete a role by ID
     * @param roleId the role ID
     * @return true if deleted, false if not found
     */
    boolean deleteRole(UUID roleId);
    
    /**
     * Assign a permission to a role
     * @param roleId the role ID
     * @param permissionId the permission ID
     * @return true if assigned, false if either not found
     */
    boolean assignPermissionToRole(UUID roleId, UUID permissionId);
    
    /**
     * Remove a permission from a role
     * @param roleId the role ID
     @param permissionId the permission ID
     * @return true if removed, false if either not found
     */
    boolean removePermissionFromRole(UUID roleId, UUID permissionId);
}
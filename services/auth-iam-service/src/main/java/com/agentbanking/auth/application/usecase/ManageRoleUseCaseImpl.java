package com.agentbanking.auth.application.usecase;

import com.agentbanking.auth.domain.model.RoleRecord;
import com.agentbanking.auth.domain.model.PermissionRecord;
import com.agentbanking.auth.domain.port.in.ManageRoleUseCase;
import com.agentbanking.auth.domain.port.out.PermissionRepository;
import com.agentbanking.auth.domain.port.out.RoleRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Use case implementation for role management
 */
public class ManageRoleUseCaseImpl implements ManageRoleUseCase {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public ManageRoleUseCaseImpl(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public RoleRecord createRole(RoleRecord roleRecord) {
        // Check if role name already exists
        if (roleRepository.findByName(roleRecord.roleName()).isPresent()) {
            throw new IllegalArgumentException("Role name already exists");
        }

        // Create role record with proper defaults
        RoleRecord newRole = new RoleRecord(
                UUID.randomUUID(),
                roleRecord.roleName(),
                roleRecord.description(),
                roleRecord.isActive(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                roleRecord.createdBy()
        );

        return roleRepository.save(newRole);
    }

    @Override
    public RoleRecord getRoleById(UUID roleId) {
        return roleRepository.findById(roleId).orElse(null);
    }

    @Override
    public RoleRecord getRoleByName(String roleName) {
        return roleRepository.findByName(roleName).orElse(null);
    }

    @Override
    public RoleRecord updateRole(UUID roleId, RoleRecord roleRecord) {
        RoleRecord existing = roleRepository.findById(roleId).orElse(null);
        if (existing == null) {
            return null;
        }

        // Check if new role name conflicts with other roles
        if (!roleRecord.roleName().equals(existing.roleName()) && 
            roleRepository.findByName(roleRecord.roleName()).isPresent()) {
            throw new IllegalArgumentException("Role name already exists");
        }

        // Update fields while preserving immutable ones
        RoleRecord updatedRole = new RoleRecord(
                existing.roleId(),
                roleRecord.roleName(),
                roleRecord.description(),
                roleRecord.isActive(),
                existing.createdAt(),
                LocalDateTime.now(),
                existing.createdBy()
        );

        return roleRepository.save(updatedRole);
    }

    @Override
    public boolean deleteRole(UUID roleId) {
        return roleRepository.deleteById(roleId);
    }

    @Override
    public boolean assignPermissionToRole(UUID roleId, UUID permissionId) {
        // Check if role exists
        RoleRecord role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            throw new IllegalArgumentException("Role not found");
        }

        // Check if permission exists
        PermissionRecord permission = permissionRepository.findById(permissionId).orElse(null);
        if (permission == null) {
            throw new IllegalArgumentException("Permission not found");
        }

        // In a full implementation, we would have a role_permission repository
        // For now, we'll return true to indicate the operation would succeed
        // A proper implementation would save the relationship
        return true;
    }

    @Override
    public boolean removePermissionFromRole(UUID roleId, UUID permissionId) {
        // Check if role exists
        RoleRecord role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            throw new IllegalArgumentException("Role not found");
        }

        // Check if permission exists
        PermissionRecord permission = permissionRepository.findById(permissionId).orElse(null);
        if (permission == null) {
            throw new IllegalArgumentException("Permission not found");
        }

        // In a full implementation, we would remove the relationship
        // For now, we'll return true to indicate the operation would succeed
        return true;
    }
}
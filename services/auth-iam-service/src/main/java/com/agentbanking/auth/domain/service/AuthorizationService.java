package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.RoleRecord;
import com.agentbanking.auth.domain.model.PermissionRecord;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.port.in.ManageRoleUseCase;
import com.agentbanking.auth.domain.port.in.ManagePermissionUseCase;
import com.agentbanking.auth.domain.port.in.CheckPermissionUseCase;
import com.agentbanking.auth.domain.port.out.RoleRepository;
import com.agentbanking.auth.domain.port.out.PermissionRepository;
import com.agentbanking.auth.domain.port.out.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain service for authorization and access control
 */
public class AuthorizationService implements ManageRoleUseCase, ManagePermissionUseCase, CheckPermissionUseCase {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public AuthorizationService(RoleRepository roleRepository,
                                PermissionRepository permissionRepository,
                                UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    // ManageRoleUseCase methods
    @Override
    public RoleRecord createRole(RoleRecord roleRecord) {
        if (roleRepository.findByName(roleRecord.roleName()).isPresent()) {
            throw new IllegalArgumentException("Role name already exists");
        }

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

        if (!roleRecord.roleName().equals(existing.roleName()) &&
            roleRepository.findByName(roleRecord.roleName()).isPresent()) {
            throw new IllegalArgumentException("Role name already exists");
        }

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
        RoleRecord role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            throw new IllegalArgumentException("Role not found");
        }

        PermissionRecord permission = permissionRepository.findById(permissionId).orElse(null);
        if (permission == null) {
            throw new IllegalArgumentException("Permission not found");
        }

        // In a full implementation, we would have a role_permission repository
        // For now, return true to indicate the operation conceptually works
        return true;
    }

    @Override
    public boolean removePermissionFromRole(UUID roleId, UUID permissionId) {
        RoleRecord role = roleRepository.findById(roleId).orElse(null);
        if (role == null) {
            throw new IllegalArgumentException("Role not found");
        }

        PermissionRecord permission = permissionRepository.findById(permissionId).orElse(null);
        if (permission == null) {
            throw new IllegalArgumentException("Permission not found");
        }

        // In a full implementation, we would remove the relationship
        return true;
    }

    // ManagePermissionUseCase methods
    @Override
    public PermissionRecord createPermission(PermissionRecord permissionRecord) {
        if (permissionRepository.findByKey(permissionRecord.permissionKey()).isPresent()) {
            throw new IllegalArgumentException("Permission key already exists");
        }

        PermissionRecord newPermission = new PermissionRecord(
                UUID.randomUUID(),
                permissionRecord.permissionKey(),
                permissionRecord.description(),
                permissionRecord.resource(),
                permissionRecord.action(),
                permissionRecord.isActive(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                permissionRecord.createdBy()
        );

        return permissionRepository.save(newPermission);
    }

    @Override
    public PermissionRecord getPermissionById(UUID permissionId) {
        return permissionRepository.findById(permissionId).orElse(null);
    }

    @Override
    public PermissionRecord getPermissionByKey(String permissionKey) {
        return permissionRepository.findByKey(permissionKey).orElse(null);
    }

    @Override
    public PermissionRecord updatePermission(UUID permissionId, PermissionRecord permissionRecord) {
        PermissionRecord existing = permissionRepository.findById(permissionId).orElse(null);
        if (existing == null) {
            return null;
        }

        if (!permissionRecord.permissionKey().equals(existing.permissionKey()) &&
            permissionRepository.findByKey(permissionRecord.permissionKey()).isPresent()) {
            throw new IllegalArgumentException("Permission key already exists");
        }

        PermissionRecord updatedPermission = new PermissionRecord(
                existing.permissionId(),
                permissionRecord.permissionKey(),
                permissionRecord.description(),
                permissionRecord.resource(),
                permissionRecord.action(),
                permissionRecord.isActive(),
                existing.createdAt(),
                LocalDateTime.now(),
                existing.createdBy()
        );

        return permissionRepository.save(updatedPermission);
    }

    @Override
    public boolean deletePermission(UUID permissionId) {
        return permissionRepository.deleteById(permissionId);
    }

    // CheckPermissionUseCase method
    @Override
    public boolean hasPermission(UUID userId, String permissionKey) {
        UserRecord user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        // Check if user has the permission directly
        if (user.permissions() != null && user.permissions().contains(permissionKey)) {
            return true;
        }

        // In a full implementation, we would check permissions through roles
        // For now, return false if not found directly
        return false;
    }
}
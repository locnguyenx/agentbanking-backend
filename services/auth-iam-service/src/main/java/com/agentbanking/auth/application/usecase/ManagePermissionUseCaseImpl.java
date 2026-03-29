package com.agentbanking.auth.application.usecase;

import com.agentbanking.auth.domain.model.PermissionRecord;
import com.agentbanking.auth.domain.port.in.ManagePermissionUseCase;
import com.agentbanking.auth.domain.port.out.PermissionRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Use case implementation for permission management
 */
public class ManagePermissionUseCaseImpl implements ManagePermissionUseCase {

    private final PermissionRepository permissionRepository;

    public ManagePermissionUseCaseImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public PermissionRecord createPermission(PermissionRecord permissionRecord) {
        // Check if permission key already exists
        if (permissionRepository.findByKey(permissionRecord.permissionKey()).isPresent()) {
            throw new IllegalArgumentException("Permission key already exists");
        }

        // Create permission record with proper defaults
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

        // Check if new permission key conflicts with other permissions
        if (!permissionRecord.permissionKey().equals(existing.permissionKey()) && 
            permissionRepository.findByKey(permissionRecord.permissionKey()).isPresent()) {
            throw new IllegalArgumentException("Permission key already exists");
        }

        // Update fields while preserving immutable ones
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
}
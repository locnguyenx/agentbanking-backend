package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.PermissionRecord;
import com.agentbanking.auth.domain.port.out.PermissionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of PermissionRepository port
 */
@Repository
public class PermissionRepositoryImpl implements PermissionRepository {

    private final PermissionJpaRepository permissionJpaRepository;
    private final PermissionMapper permissionMapper;

    public PermissionRepositoryImpl(PermissionJpaRepository permissionJpaRepository, PermissionMapper permissionMapper) {
        this.permissionJpaRepository = permissionJpaRepository;
        this.permissionMapper = permissionMapper;
    }

    @Override
    public Optional<PermissionRecord> findById(UUID permissionId) {
        return permissionJpaRepository.findById(permissionId)
                .map(permissionMapper::toRecord);
    }

    @Override
    public Optional<PermissionRecord> findByKey(String permissionKey) {
        return permissionJpaRepository.findByPermissionKey(permissionKey)
                .map(permissionMapper::toRecord);
    }

    @Override
    public PermissionRecord save(PermissionRecord permissionRecord) {
        PermissionEntity entity = permissionMapper.toEntity(permissionRecord);
        PermissionEntity saved = permissionJpaRepository.save(entity);
        return permissionMapper.toRecord(saved);
    }

    @Override
    public boolean deleteById(UUID permissionId) {
        if (permissionJpaRepository.existsById(permissionId)) {
            permissionJpaRepository.deleteById(permissionId);
            return true;
        }
        return false;
    }
}
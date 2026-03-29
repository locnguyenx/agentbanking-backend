package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.RoleRecord;
import com.agentbanking.auth.domain.port.out.RoleRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of RoleRepository port
 */
@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RoleJpaRepository roleJpaRepository;
    private final RoleMapper roleMapper;

    public RoleRepositoryImpl(RoleJpaRepository roleJpaRepository, RoleMapper roleMapper) {
        this.roleJpaRepository = roleJpaRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    public Optional<RoleRecord> findById(UUID roleId) {
        return roleJpaRepository.findById(roleId)
                .map(roleMapper::toRecord);
    }

    @Override
    public Optional<RoleRecord> findByName(String roleName) {
        return roleJpaRepository.findByRoleName(roleName)
                .map(roleMapper::toRecord);
    }

    @Override
    public RoleRecord save(RoleRecord roleRecord) {
        RoleEntity entity = roleMapper.toEntity(roleRecord);
        RoleEntity saved = roleJpaRepository.save(entity);
        return roleMapper.toRecord(saved);
    }

    @Override
    public boolean deleteById(UUID roleId) {
        if (roleJpaRepository.existsById(roleId)) {
            roleJpaRepository.deleteById(roleId);
            return true;
        }
        return false;
    }
}
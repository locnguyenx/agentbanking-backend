package com.agentbanking.onboarding.infrastructure.persistence.repository;

import com.agentbanking.onboarding.domain.model.UserRecord;
import com.agentbanking.onboarding.domain.model.UserStatus;
import com.agentbanking.onboarding.domain.port.out.UserRepository;
import com.agentbanking.onboarding.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of UserRepository
 * Note: Authentication and IAM logic (passwordHash, role, permissions, lastLoginAt) has been moved to auth-iam-service
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;

    public UserRepositoryImpl(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UserRecord save(UserRecord user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return toRecord(saved);
    }

    @Override
    public Optional<UserRecord> findById(UUID userId) {
        return jpaRepository.findById(userId).map(this::toRecord);
    }

    @Override
    public Optional<UserRecord> findByUsername(String username) {
        return jpaRepository.findByUsername(username).map(this::toRecord);
    }

    @Override
    public Page<UserRecord> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable).map(this::toRecord);
    }

    @Override
    public void deleteById(UUID userId) {
        jpaRepository.deleteById(userId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    private UserEntity toEntity(UserRecord record) {
        UserEntity entity = new UserEntity();
        entity.setUserId(record.userId());
        entity.setUsername(record.username());
        entity.setEmail(record.email());
        entity.setFullName(record.fullName());
        entity.setStatus(record.status());
        entity.setCreatedAt(record.createdAt() != null ? record.createdAt() : LocalDateTime.now());
        entity.setUpdatedAt(record.updatedAt());
        entity.setCreatedBy(record.createdBy());
        return entity;
    }

    private UserRecord toRecord(UserEntity entity) {
        return new UserRecord(
            entity.getUserId(),
            entity.getUsername(),
            entity.getEmail(),
            entity.getFullName(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getCreatedBy()
        );
    }
}
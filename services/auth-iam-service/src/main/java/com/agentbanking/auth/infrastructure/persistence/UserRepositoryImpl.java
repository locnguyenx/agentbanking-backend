package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.port.out.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA implementation of UserRepository port
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    public UserRepositoryImpl(UserJpaRepository userJpaRepository, UserMapper userMapper) {
        this.userJpaRepository = userJpaRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Optional<UserRecord> findById(UUID userId) {
        return userJpaRepository.findById(userId)
                .map(userMapper::toRecord);
    }

    @Override
    public Optional<UserRecord> findByUsername(String username) {
        return userJpaRepository.findByUsername(username)
                .map(userMapper::toRecord);
    }

    @Override
    public Optional<UserRecord> findByEmail(String email) {
        return userJpaRepository.findByEmail(email)
                .map(userMapper::toRecord);
    }

    @Override
    public UserRecord save(UserRecord userRecord) {
        UserEntity entity = userMapper.toEntity(userRecord);
        UserEntity saved = userJpaRepository.save(entity);
        return userMapper.toRecord(saved);
    }

    @Override
    public boolean deleteById(UUID userId) {
        if (userJpaRepository.existsById(userId)) {
            userJpaRepository.deleteById(userId);
            return true;
        }
        return false;
    }

    @Override
    public List<UserRecord> findAll() {
        return userJpaRepository.findAll().stream()
                .map(userMapper::toRecord)
                .collect(Collectors.toList());
    }
}
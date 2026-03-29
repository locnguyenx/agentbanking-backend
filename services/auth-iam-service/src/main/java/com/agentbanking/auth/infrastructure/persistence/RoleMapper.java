package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.RoleRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper to convert between RoleEntity and RoleRecord
 */
@Component
public class RoleMapper {

    public RoleRecord toRecord(RoleEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RoleRecord(
                entity.getRoleId(),
                entity.getRoleName(),
                entity.getDescription(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy()
        );
    }

    public RoleEntity toEntity(RoleRecord record) {
        if (record == null) {
            return null;
        }
        RoleEntity entity = new RoleEntity();
        entity.setRoleId(record.roleId());
        entity.setRoleName(record.roleName());
        entity.setDescription(record.description());
        entity.setIsActive(record.isActive());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        entity.setCreatedBy(record.createdBy());
        return entity;
    }
}
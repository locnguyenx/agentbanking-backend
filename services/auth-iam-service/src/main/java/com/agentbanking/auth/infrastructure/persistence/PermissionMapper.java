package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.PermissionRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper to convert between PermissionEntity and PermissionRecord
 */
@Component
public class PermissionMapper {

    public PermissionRecord toRecord(PermissionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PermissionRecord(
                entity.getPermissionId(),
                entity.getPermissionKey(),
                entity.getDescription(),
                entity.getResource(),
                entity.getAction(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy()
        );
    }

    public PermissionEntity toEntity(PermissionRecord record) {
        if (record == null) {
            return null;
        }
        PermissionEntity entity = new PermissionEntity();
        entity.setPermissionId(record.permissionId());
        entity.setPermissionKey(record.permissionKey());
        entity.setDescription(record.description());
        entity.setResource(record.resource());
        entity.setAction(record.action());
        entity.setIsActive(record.isActive());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        entity.setCreatedBy(record.createdBy());
        return entity;
    }
}
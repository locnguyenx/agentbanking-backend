package com.agentbanking.onboarding.infrastructure.persistence.mapper;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.infrastructure.persistence.entity.AgentEntity;
import org.springframework.stereotype.Component;

@Component
public class AgentMapper {

    public AgentRecord toRecord(AgentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AgentRecord(
            entity.getAgentId(),
            entity.getAgentCode(),
            entity.getBusinessName(),
            entity.getTier(),
            entity.getStatus(),
            entity.getMerchantGpsLat(),
            entity.getMerchantGpsLng(),
            entity.getMykadNumber(),
            entity.getPhoneNumber(),
            entity.getUserCreationStatus(),
            entity.getUserCreationError(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public AgentEntity toEntity(AgentRecord record) {
        if (record == null) {
            return null;
        }
        AgentEntity entity = new AgentEntity();
        entity.setAgentId(record.agentId());
        entity.setAgentCode(record.agentCode());
        entity.setBusinessName(record.businessName());
        entity.setTier(record.tier());
        entity.setStatus(record.status());
        entity.setMerchantGpsLat(record.merchantGpsLat());
        entity.setMerchantGpsLng(record.merchantGpsLng());
        entity.setMykadNumber(record.mykadNumber());
        entity.setPhoneNumber(record.phoneNumber());
        entity.setUserCreationStatus(record.userCreationStatus());
        entity.setUserCreationError(record.userCreationError());
        entity.setCreatedAt(record.createdAt());
        entity.setUpdatedAt(record.updatedAt());
        return entity;
    }
}

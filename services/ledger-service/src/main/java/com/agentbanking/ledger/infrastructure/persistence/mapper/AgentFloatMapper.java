package com.agentbanking.ledger.infrastructure.persistence.mapper;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;
import com.agentbanking.ledger.infrastructure.persistence.entity.AgentFloatEntity;
import com.agentbanking.ledger.infrastructure.persistence.entity.AgentFloatEntity;

public class AgentFloatMapper {
    
    public static AgentFloatRecord toRecord(AgentFloatEntity entity) {
        if (entity == null) return null;
        return new AgentFloatRecord(
            entity.getFloatId(),
            entity.getAgentId(),
            entity.getBalance(),
            entity.getReservedBalance(),
            entity.getCurrency(),
            entity.getVersion()
        );
    }
    
    public static AgentFloatEntity toEntity(AgentFloatRecord record) {
        if (record == null) return null;
        AgentFloatEntity entity = new AgentFloatEntity();
        entity.setFloatId(record.floatId());
        entity.setAgentId(record.agentId());
        entity.setBalance(record.balance());
        entity.setReservedBalance(record.reservedBalance());
        entity.setCurrency(record.currency());
        entity.setVersion(record.version());
        return entity;
    }
    
    public static void updateEntity(AgentFloatRecord record, AgentFloatEntity entity) {
        entity.setBalance(record.balance());
        entity.setReservedBalance(record.reservedBalance());
        entity.setCurrency(record.currency());
    }
}

package com.agentbanking.rules.infrastructure.persistence.mapper;

import com.agentbanking.rules.domain.model.VelocityRuleRecord;
import com.agentbanking.rules.infrastructure.persistence.entity.VelocityRuleEntity;

public class VelocityRuleMapper {

    public static VelocityRuleRecord toDomain(VelocityRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        return new VelocityRuleRecord(
            entity.getRuleId(),
            entity.getRuleName(),
            entity.getMaxTransactionsPerDay(),
            entity.getMaxAmountPerDay(),
            entity.getScope(),
            entity.getTransactionType(),
            entity.isActive()
        );
    }

    public static VelocityRuleEntity toEntity(VelocityRuleRecord record) {
        if (record == null) {
            return null;
        }
        VelocityRuleEntity entity = new VelocityRuleEntity();
        entity.setRuleId(record.ruleId());
        entity.setRuleName(record.ruleName());
        entity.setMaxTransactionsPerDay(record.maxTransactionsPerDay());
        entity.setMaxAmountPerDay(record.maxAmountPerDay());
        entity.setScope(record.scope());
        entity.setTransactionType(record.transactionType());
        entity.setActive(record.active());
        return entity;
    }
}

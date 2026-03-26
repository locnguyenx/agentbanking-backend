package com.agentbanking.rules.infrastructure.persistence.mapper;

import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.infrastructure.persistence.entity.FeeConfigEntity;

public class FeeConfigMapper {

    public static FeeConfigRecord toDomain(FeeConfigEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FeeConfigRecord(
            entity.getFeeConfigId(),
            entity.getTransactionType(),
            entity.getAgentTier(),
            entity.getFeeType(),
            entity.getCustomerFeeValue(),
            entity.getAgentCommissionValue(),
            entity.getBankShareValue(),
            entity.getDailyLimitAmount(),
            entity.getDailyLimitCount(),
            entity.getEffectiveFrom(),
            entity.getEffectiveTo()
        );
    }

    public static FeeConfigEntity toEntity(FeeConfigRecord record) {
        if (record == null) {
            return null;
        }
        FeeConfigEntity entity = new FeeConfigEntity();
        entity.setFeeConfigId(record.feeConfigId());
        entity.setTransactionType(record.transactionType());
        entity.setAgentTier(record.agentTier());
        entity.setFeeType(record.feeType());
        entity.setCustomerFeeValue(record.customerFeeValue());
        entity.setAgentCommissionValue(record.agentCommissionValue());
        entity.setBankShareValue(record.bankShareValue());
        entity.setDailyLimitAmount(record.dailyLimitAmount());
        entity.setDailyLimitCount(record.dailyLimitCount());
        entity.setEffectiveFrom(record.effectiveFrom());
        entity.setEffectiveTo(record.effectiveTo());
        return entity;
    }
}

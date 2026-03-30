package com.agentbanking.rules.domain.port.in;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.rules.domain.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CreateFeeConfigUseCase {

    CreateFeeConfigResult createFeeConfig(CreateFeeConfigCommand command);

    record CreateFeeConfigCommand(
        TransactionType transactionType,
        AgentTier agentTier,
        FeeType feeType,
        BigDecimal customerFeeValue,
        BigDecimal agentCommissionValue,
        BigDecimal bankShareValue,
        BigDecimal dailyLimitAmount,
        Integer dailyLimitCount,
        LocalDate effectiveFrom,
        LocalDate effectiveTo
    ) {}

    record CreateFeeConfigResult(
        UUID feeConfigId,
        TransactionType transactionType,
        AgentTier agentTier,
        String status
    ) {}
}

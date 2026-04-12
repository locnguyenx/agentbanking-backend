package com.agentbanking.orchestrator.application.activity;

import com.agentbanking.orchestrator.domain.port.out.MerchantTransactionPort.MerchantTransactionResult;
import io.temporal.activity.ActivityInterface;
import java.util.UUID;


@ActivityInterface
public interface CreateMerchantTransactionRecordActivity {
    MerchantTransactionResult create(UUID transactionId, String merchantType, java.math.BigDecimal grossAmount, 
        java.math.BigDecimal mdrRate, java.math.BigDecimal mdrAmount, java.math.BigDecimal netCreditToFloat, String receiptType);
}

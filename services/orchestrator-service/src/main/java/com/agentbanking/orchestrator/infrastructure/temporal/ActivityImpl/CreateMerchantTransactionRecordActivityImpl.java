package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.CreateMerchantTransactionRecordActivity;
import com.agentbanking.orchestrator.domain.port.out.MerchantTransactionPort;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@ActivityImpl(workers = "agent-banking-tasks")
public class CreateMerchantTransactionRecordActivityImpl implements CreateMerchantTransactionRecordActivity {

    private final MerchantTransactionPort port;

    public CreateMerchantTransactionRecordActivityImpl(MerchantTransactionPort port) {
        this.port = port;
    }

    @Override
    public MerchantTransactionPort.MerchantTransactionResult create(UUID transactionId, String merchantType, 
            BigDecimal grossAmount, BigDecimal mdrRate, BigDecimal mdrAmount, BigDecimal netCreditToFloat, String receiptType) {
        return port.createRecord(new MerchantTransactionPort.MerchantTransactionRecord(
            transactionId, merchantType, grossAmount, mdrRate, mdrAmount, netCreditToFloat, receiptType
        ));
    }
}

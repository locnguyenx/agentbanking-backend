package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.MerchantTransactionPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public class MerchantTransactionAdapter implements MerchantTransactionPort {

    private static final Logger log = LoggerFactory.getLogger(MerchantTransactionAdapter.class);

    private final MerchantTransactionClient client;

    public MerchantTransactionAdapter(MerchantTransactionClient client) {
        this.client = client;
    }

    @Override
    public MerchantTransactionResult createRecord(MerchantTransactionRecord record) {
        log.info("Creating merchant transaction record for: {}", record.transactionId());
        var request = new MerchantTransactionClient.MerchantTransactionRequest(
            record.transactionId(),
            record.merchantType(),
            record.grossAmount(),
            record.mdrRate(),
            record.mdrAmount(),
            record.netCreditToFloat(),
            record.receiptType()
        );
        var response = client.createRecord(request);
        return new MerchantTransactionResult(response.success(), response.recordId(), response.errorCode());
    }
}

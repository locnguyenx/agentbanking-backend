package com.agentbanking.switchadapter.application.usecase;

import com.agentbanking.switchadapter.domain.port.in.BalanceInquiryUseCase;
import com.agentbanking.switchadapter.domain.service.SwitchAdapterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class BalanceInquiryUseCaseImpl implements BalanceInquiryUseCase {

    private static final Logger log = LoggerFactory.getLogger(BalanceInquiryUseCaseImpl.class);

    private final SwitchAdapterService switchAdapterService;

    public BalanceInquiryUseCaseImpl(SwitchAdapterService switchAdapterService) {
        this.switchAdapterService = switchAdapterService;
    }

    @Override
    public BalanceInquiryResult inquiryBalance(UUID internalTransactionId, String encryptedCardData, String pinBlock) {
        log.info("Processing balance inquiry for transaction: {}", internalTransactionId);

        var record = switchAdapterService.processBalanceInquiry(internalTransactionId, encryptedCardData, pinBlock);

        BigDecimal simulatedBalance = new BigDecimal("5000.00");

        return new BalanceInquiryResult(
            record.switchTxId(),
            "SUCCESS",
            "00",
            record.switchReference(),
            simulatedBalance,
            "MYR",
            "411111******1111"
        );
    }
}

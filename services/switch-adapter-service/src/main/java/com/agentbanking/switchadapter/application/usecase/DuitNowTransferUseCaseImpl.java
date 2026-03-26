package com.agentbanking.switchadapter.application.usecase;

import com.agentbanking.switchadapter.domain.model.MessageType;
import com.agentbanking.switchadapter.domain.model.SwitchStatus;
import com.agentbanking.switchadapter.domain.model.SwitchTransactionRecord;
import com.agentbanking.switchadapter.domain.port.in.DuitNowTransferUseCase;
import com.agentbanking.switchadapter.domain.port.out.SwitchTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DuitNowTransferUseCaseImpl implements DuitNowTransferUseCase {

    private final SwitchTransactionRepository repository;

    public DuitNowTransferUseCaseImpl(SwitchTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public DuitNowTransferResult transferDuitNow(UUID internalTransactionId, String proxyType, String proxyValue, BigDecimal amount) {
        BigDecimal roundedAmount = amount.setScale(2, RoundingMode.HALF_UP);

        SwitchTransactionRecord record = new SwitchTransactionRecord(
            UUID.randomUUID(),
            internalTransactionId,
            MessageType.ISO20022,
            "ACSC",
            "DN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            SwitchStatus.APPROVED,
            null,
            0,
            roundedAmount,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        repository.save(record);

        return new DuitNowTransferResult(
            record.switchTxId(),
            "SETTLED",
            record.isoResponseCode(),
            record.switchReference()
        );
    }
}
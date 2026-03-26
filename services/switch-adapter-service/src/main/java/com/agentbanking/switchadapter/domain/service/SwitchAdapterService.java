package com.agentbanking.switchadapter.domain.service;

import com.agentbanking.switchadapter.domain.model.MessageType;
import com.agentbanking.switchadapter.domain.model.SwitchStatus;
import com.agentbanking.switchadapter.domain.model.SwitchTransactionRecord;
import com.agentbanking.switchadapter.domain.port.out.SwitchTransactionRepository;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

public class SwitchAdapterService {

    private final SwitchTransactionRepository repository;

    public SwitchAdapterService(SwitchTransactionRepository repository) {
        this.repository = repository;
    }

    public SwitchTransactionRecord processCardAuth(UUID internalTransactionId,
                                                   String pan,
                                                   java.math.BigDecimal amount) {
        java.math.BigDecimal roundedAmount = amount.setScale(2, RoundingMode.HALF_UP);

        SwitchTransactionRecord record = new SwitchTransactionRecord(
            UUID.randomUUID(),
            internalTransactionId,
            MessageType.MT0100,
            "00",
            "PAYNET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            SwitchStatus.APPROVED,
            null,
            0,
            roundedAmount,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        repository.save(record);
        return record;
    }

    public SwitchTransactionRecord processReversal(UUID originalTransactionId,
                                                   String originalReference,
                                                   java.math.BigDecimal amount) {
        java.math.BigDecimal roundedAmount = amount.setScale(2, RoundingMode.HALF_UP);

        SwitchTransactionRecord record = new SwitchTransactionRecord(
            UUID.randomUUID(),
            originalTransactionId,
            MessageType.MT0400,
            "00",
            "REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            SwitchStatus.REVERSED,
            originalReference,
            1,
            roundedAmount,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        repository.save(record);
        return record;
    }

    public SwitchTransactionRecord processDuitNowTransfer(UUID internalTransactionId,
                                                           String proxyType,
                                                           String proxyValue,
                                                           java.math.BigDecimal amount) {
        java.math.BigDecimal roundedAmount = amount.setScale(2, RoundingMode.HALF_UP);

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
        return record;
    }
}
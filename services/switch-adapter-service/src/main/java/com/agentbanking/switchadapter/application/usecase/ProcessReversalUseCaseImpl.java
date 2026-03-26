package com.agentbanking.switchadapter.application.usecase;

import com.agentbanking.switchadapter.domain.model.MessageType;
import com.agentbanking.switchadapter.domain.model.SwitchStatus;
import com.agentbanking.switchadapter.domain.model.SwitchTransactionRecord;
import com.agentbanking.switchadapter.domain.port.in.ProcessReversalUseCase;
import com.agentbanking.switchadapter.domain.port.out.SwitchTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ProcessReversalUseCaseImpl implements ProcessReversalUseCase {

    private final SwitchTransactionRepository repository;

    public ProcessReversalUseCaseImpl(SwitchTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ProcessReversalResult processReversal(UUID originalTransactionId, String originalReference, BigDecimal amount) {
        BigDecimal roundedAmount = amount.setScale(2, RoundingMode.HALF_UP);

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

        return new ProcessReversalResult(
            record.switchTxId(),
            "REVERSED",
            record.isoResponseCode(),
            record.switchReference()
        );
    }
}
package com.agentbanking.switchadapter.application.usecase;

import com.agentbanking.switchadapter.domain.model.MessageType;
import com.agentbanking.switchadapter.domain.model.SwitchStatus;
import com.agentbanking.switchadapter.domain.model.SwitchTransactionRecord;
import com.agentbanking.switchadapter.domain.port.in.AuthorizeTransactionUseCase;
import com.agentbanking.switchadapter.domain.port.out.SwitchTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthorizeTransactionUseCaseImpl implements AuthorizeTransactionUseCase {

    private final SwitchTransactionRepository repository;

    public AuthorizeTransactionUseCaseImpl(SwitchTransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AuthorizeTransactionResult authorizeTransaction(UUID internalTransactionId, String pan, BigDecimal amount) {
        BigDecimal roundedAmount = amount.setScale(2, RoundingMode.HALF_UP);

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

        return new AuthorizeTransactionResult(
            record.switchTxId(),
            "APPROVED",
            record.isoResponseCode(),
            record.switchReference()
        );
    }
}
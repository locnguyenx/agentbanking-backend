package com.agentbanking.biller.application.usecase;

import com.agentbanking.biller.domain.model.PaymentStatus;
import com.agentbanking.biller.domain.model.TopupTransaction;
import com.agentbanking.biller.domain.model.TopupTransactionRecord;
import com.agentbanking.biller.domain.port.in.ProcessTopupUseCase;
import com.agentbanking.biller.domain.port.out.TopupTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ProcessTopupUseCaseImpl implements ProcessTopupUseCase {

    private final TopupTransactionRepository topupTransactionRepository;

    public ProcessTopupUseCaseImpl(TopupTransactionRepository topupTransactionRepository) {
        this.topupTransactionRepository = topupTransactionRepository;
    }

    @Override
    @Transactional
    public ProcessTopupResult processTopup(String telco, String phoneNumber, BigDecimal amount, UUID internalTransactionId) {
        TopupTransactionRecord record = new TopupTransactionRecord(
            UUID.randomUUID(),
            internalTransactionId,
            telco,
            phoneNumber,
            amount,
            PaymentStatus.PAID,
            telco + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        TopupTransactionRecord saved = topupTransactionRepository.save(record);

        return new ProcessTopupResult(
            saved.topupId(),
            "COMPLETED",
            saved.telcoReference(),
            saved.amount()
        );
    }
}
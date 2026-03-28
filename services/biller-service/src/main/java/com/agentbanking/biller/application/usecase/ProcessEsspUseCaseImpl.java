package com.agentbanking.biller.application.usecase;

import com.agentbanking.biller.domain.model.EsspTransactionRecord;
import com.agentbanking.biller.domain.port.in.ProcessEsspUseCase;
import com.agentbanking.biller.domain.port.out.BillerConfigRepository;
import com.agentbanking.biller.domain.port.out.BillPaymentRepository;
import com.agentbanking.biller.domain.port.out.EWalletTransactionRepository;
import com.agentbanking.biller.domain.port.out.EsspTransactionRepository;
import com.agentbanking.biller.domain.port.out.TopupTransactionRepository;
import com.agentbanking.biller.domain.service.BillerService;
import org.springframework.stereotype.Component;

/**
 * Implementation of ProcessEsspUseCase
 */
@Component
public class ProcessEsspUseCaseImpl implements ProcessEsspUseCase {

    private final BillerService billerService;

    public ProcessEsspUseCaseImpl(BillerService billerService) {
        this.billerService = billerService;
    }

    @Override
    public EsspTransactionResult processEsspPurchase(EsspTransactionCommand command) {
        EsspTransactionRecord record = billerService.processEsspPurchase(
                command.amount(),
                command.internalTransactionId()
        );

        return new EsspTransactionResult(
                record.status().toString(),
                record.transactionId().toString(),
                record.esspCertificateNumber()
        );
    }
}
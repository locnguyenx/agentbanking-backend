package com.agentbanking.biller.application.usecase;

import com.agentbanking.biller.domain.model.EWalletTransactionRecord;
import com.agentbanking.biller.domain.port.in.ProcessEWalletUseCase;
import com.agentbanking.biller.domain.port.out.BillerConfigRepository;
import com.agentbanking.biller.domain.port.out.EWalletTransactionRepository;
import com.agentbanking.biller.domain.port.out.EsspTransactionRepository;
import com.agentbanking.biller.domain.port.out.TopupTransactionRepository;
import com.agentbanking.biller.domain.service.BillerService;
import org.springframework.stereotype.Component;

/**
 * Implementation of ProcessEWalletUseCase
 */
@Component
public class ProcessEWalletUseCaseImpl implements ProcessEWalletUseCase {

    private final BillerService billerService;

    public ProcessEWalletUseCaseImpl(BillerService billerService) {
        this.billerService = billerService;
    }

    @Override
    public EWalletTransactionResult processEWalletTransaction(EWalletTransactionCommand command) {
        EWalletTransactionRecord record = billerService.processEWalletTransaction(
                command.walletProvider(),
                command.walletId(),
                command.amount(),
                command.internalTransactionId(),
                command.isWithdrawal()
        );

        return new EWalletTransactionResult(
                record.status().toString(),
                record.transactionId().toString(),
                record.walletReference()
        );
    }
}
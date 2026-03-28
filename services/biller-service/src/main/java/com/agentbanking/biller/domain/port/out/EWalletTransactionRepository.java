package com.agentbanking.biller.domain.port.out;

import com.agentbanking.biller.domain.model.EWalletTransactionRecord;
import java.util.UUID;

/**
 * Repository port for e-Wallet transactions
 */
public interface EWalletTransactionRepository {
    EWalletTransactionRecord save(EWalletTransactionRecord transaction);
}
package com.agentbanking.biller.domain.port.out;

import com.agentbanking.biller.domain.model.EsspTransactionRecord;
import java.util.UUID;

/**
 * Repository port for eSSP transactions
 */
public interface EsspTransactionRepository {
    EsspTransactionRecord save(EsspTransactionRecord transaction);
}
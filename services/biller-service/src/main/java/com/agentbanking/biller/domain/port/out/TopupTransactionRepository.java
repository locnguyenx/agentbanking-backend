package com.agentbanking.biller.domain.port.out;

import com.agentbanking.biller.domain.model.TopupTransactionRecord;
import java.util.UUID;

public interface TopupTransactionRepository {
    TopupTransactionRecord save(TopupTransactionRecord transaction);
}
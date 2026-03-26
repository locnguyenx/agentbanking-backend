package com.agentbanking.switchadapter.domain.port.out;

import com.agentbanking.switchadapter.domain.model.SwitchTransactionRecord;

import java.util.UUID;

public interface SwitchTransactionRepository {
    void save(SwitchTransactionRecord record);
    SwitchTransactionRecord findById(UUID switchTxId);
}

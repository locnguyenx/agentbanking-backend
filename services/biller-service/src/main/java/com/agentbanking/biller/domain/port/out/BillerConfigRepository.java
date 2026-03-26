package com.agentbanking.biller.domain.port.out;

import com.agentbanking.biller.domain.model.BillerConfigRecord;
import java.util.Optional;
import java.util.UUID;

public interface BillerConfigRepository {
    Optional<BillerConfigRecord> findByBillerCodeAndActiveTrue(String billerCode);
    BillerConfigRecord save(BillerConfigRecord config);
}
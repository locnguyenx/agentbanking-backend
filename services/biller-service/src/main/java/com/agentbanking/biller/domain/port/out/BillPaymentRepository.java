package com.agentbanking.biller.domain.port.out;

import com.agentbanking.biller.domain.model.BillPaymentRecord;
import java.util.UUID;

public interface BillPaymentRepository {
    BillPaymentRecord save(BillPaymentRecord payment);
}
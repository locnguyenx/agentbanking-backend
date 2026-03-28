package com.agentbanking.rules.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransactionCountPort {
    BigDecimal getDailyAmountTotal(String customerMykad, String transactionType, LocalDate date);
    int getDailyCount(String customerMykad, String transactionType, LocalDate date);
}

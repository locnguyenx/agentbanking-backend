package com.agentbanking.ledger.domain.port.in;

import java.math.BigDecimal;

public interface CustomerBalanceInquiryUseCase {
    CustomerBalanceResponse inquire(CustomerInquiryCommand command);

    record CustomerInquiryCommand(String encryptedCardData, String pinBlock) {}

    record CustomerBalanceResponse(BigDecimal balance, String currency, String accountMasked) {}
}

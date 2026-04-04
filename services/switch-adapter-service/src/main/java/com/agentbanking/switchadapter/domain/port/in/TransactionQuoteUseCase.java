package com.agentbanking.switchadapter.domain.port.in;

public interface TransactionQuoteUseCase {

    QuoteResult calculateQuote(String agentId, String agentTier, String amount,
                               String serviceCode, String fundingSource, String billerRouting);

    record QuoteResult(
        String quoteId,
        String amount,
        String fee,
        String total,
        String commission
    ) {}
}

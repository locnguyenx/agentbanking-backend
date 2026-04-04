package com.agentbanking.switchadapter.infrastructure.web.dto;

import com.agentbanking.switchadapter.domain.port.in.TransactionQuoteUseCase;

public record TransactionQuoteResponse(
    String quoteId,
    String amount,
    String fee,
    String total,
    String commission
) {
    public static TransactionQuoteResponse from(TransactionQuoteUseCase.QuoteResult result) {
        return new TransactionQuoteResponse(
            result.quoteId(),
            result.amount(),
            result.fee(),
            result.total(),
            result.commission()
        );
    }
}

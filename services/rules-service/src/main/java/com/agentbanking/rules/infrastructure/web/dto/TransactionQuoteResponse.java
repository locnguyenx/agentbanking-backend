package com.agentbanking.rules.infrastructure.web.dto;

import com.agentbanking.rules.domain.port.in.TransactionQuoteUseCase;

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

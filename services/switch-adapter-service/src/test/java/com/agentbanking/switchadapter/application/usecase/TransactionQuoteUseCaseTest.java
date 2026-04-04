package com.agentbanking.switchadapter.application.usecase;

import com.agentbanking.switchadapter.domain.port.in.TransactionQuoteUseCase.QuoteResult;
import com.agentbanking.switchadapter.domain.port.out.FeeCalculationGateway;
import com.agentbanking.switchadapter.domain.port.out.FeeCalculationGateway.FeeCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionQuoteUseCaseTest {

    @Mock
    private FeeCalculationGateway feeCalculationGateway;

    private TransactionQuoteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new TransactionQuoteUseCaseImpl(feeCalculationGateway);
    }

    @Test
    void calculateQuote_shouldReturnQuoteWithFeeAndCommission() {
        String agentId = "agent-001";
        String agentTier = "STANDARD";
        String amount = "100.00";
        String serviceCode = "CASH_WITHDRAWAL";
        String fundingSource = "CARD_EMV";

        when(feeCalculationGateway.calculateFee(any(), any(), any()))
            .thenReturn(new FeeCalculationResult(
                new BigDecimal("1.00"),
                new BigDecimal("0.50"),
                new BigDecimal("0.50")
            ));

        QuoteResult result = useCase.calculateQuote(
            agentId, agentTier, amount, serviceCode, fundingSource, null
        );

        assertNotNull(result.quoteId());
        assertTrue(result.quoteId().startsWith("QT-"));
        assertEquals("100.00", result.amount());
        assertEquals("1.00", result.fee());
        assertEquals("101.00", result.total());
        assertEquals("0.50", result.commission());
    }

    @Test
    void calculateQuote_shouldThrowWhenFeeCalculationFails() {
        when(feeCalculationGateway.calculateFee(any(), any(), any()))
            .thenThrow(new RuntimeException("Fee config not found"));

        assertThrows(IllegalStateException.class, () ->
            useCase.calculateQuote("agent-001", "STANDARD",
                "100.00", "CASH_WITHDRAWAL", "CARD_EMV", null)
        );
    }
}

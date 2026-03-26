package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.AgentTier;
import com.agentbanking.rules.domain.model.FeeConfigRecord;
import com.agentbanking.rules.domain.model.FeeType;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.port.out.FeeConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeCalculationServiceTest {

    @Mock
    private FeeConfigRepository feeConfigRepository;

    private FeeCalculationService feeCalculationService;

    @BeforeEach
    void setUp() {
        feeCalculationService = new FeeCalculationService(feeConfigRepository);
    }

    @Test
    void calculate_withFixedFeeType_returnsCorrectFees() {
        FeeConfigRecord config = new FeeConfigRecord(
            UUID.randomUUID(),
            TransactionType.CASH_DEPOSIT,
            AgentTier.STANDARD,
            FeeType.FIXED,
            new BigDecimal("5.00"),
            new BigDecimal("2.00"),
            new BigDecimal("3.00"),
            BigDecimal.ZERO,
            0,
            LocalDate.now(),
            null
        );
        when(feeConfigRepository.findByTransactionTypeAndAgentTier(
            TransactionType.CASH_DEPOSIT, AgentTier.STANDARD, LocalDate.now()))
            .thenReturn(Optional.of(config));

        FeeCalculationService.FeeCalculationResult result = 
            feeCalculationService.calculate(new BigDecimal("100.00"), TransactionType.CASH_DEPOSIT, AgentTier.STANDARD);

        assertEquals(new BigDecimal("5.00"), result.customerFee());
        assertEquals(new BigDecimal("2.00"), result.agentCommission());
        assertEquals(new BigDecimal("3.00"), result.bankShare());
    }

    @Test
    void calculate_withPercentageFeeType_returnsCorrectCalculatedFees() {
        FeeConfigRecord config = new FeeConfigRecord(
            UUID.randomUUID(),
            TransactionType.CASH_DEPOSIT,
            AgentTier.STANDARD,
            FeeType.PERCENTAGE,
            new BigDecimal("0.05"),
            new BigDecimal("0.02"),
            new BigDecimal("0.03"),
            BigDecimal.ZERO,
            0,
            LocalDate.now(),
            null
        );
        when(feeConfigRepository.findByTransactionTypeAndAgentTier(
            TransactionType.CASH_DEPOSIT, AgentTier.STANDARD, LocalDate.now()))
            .thenReturn(Optional.of(config));

        FeeCalculationService.FeeCalculationResult result = 
            feeCalculationService.calculate(new BigDecimal("100.00"), TransactionType.CASH_DEPOSIT, AgentTier.STANDARD);

        assertEquals(new BigDecimal("5.00"), result.customerFee());
        assertEquals(new BigDecimal("2.00"), result.agentCommission());
        assertEquals(new BigDecimal("3.00"), result.bankShare());
    }

    @Test
    void calculate_withNoConfig_throwsException() {
        when(feeConfigRepository.findByTransactionTypeAndAgentTier(
            TransactionType.CASH_DEPOSIT, AgentTier.STANDARD, LocalDate.now()))
            .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            feeCalculationService.calculate(new BigDecimal("100.00"), TransactionType.CASH_DEPOSIT, AgentTier.STANDARD));
    }
}

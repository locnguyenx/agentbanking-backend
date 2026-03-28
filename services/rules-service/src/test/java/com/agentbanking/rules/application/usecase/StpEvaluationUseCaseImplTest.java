package com.agentbanking.rules.application.usecase;

import com.agentbanking.rules.domain.model.*;
import com.agentbanking.rules.domain.port.in.StpEvaluationUseCase;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import com.agentbanking.rules.domain.service.LimitEnforcementService;
import com.agentbanking.rules.domain.service.StpDecisionService;
import com.agentbanking.rules.domain.service.VelocityCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StpEvaluationUseCaseImplTest {

    @Mock
    private VelocityRuleRepository velocityRuleRepository;

    private VelocityCheckService velocityCheckService;
    private LimitEnforcementService limitEnforcementService;
    private StpDecisionService stpDecisionService;
    private StpEvaluationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        velocityCheckService = new VelocityCheckService(velocityRuleRepository);
        limitEnforcementService = new LimitEnforcementService();
        stpDecisionService = new StpDecisionService(velocityCheckService, limitEnforcementService);
        useCase = new StpEvaluationUseCaseImpl(stpDecisionService);
    }

    @Test
    void evaluate_mapsFullStpResponse() {
        when(velocityRuleRepository.findActiveRules()).thenReturn(java.util.List.of());

        FeeConfigRecord config = new FeeConfigRecord(
                UUID.randomUUID(), TransactionType.CASH_WITHDRAWAL, AgentTier.STANDARD,
                FeeType.FIXED, new BigDecimal("1.00"), new BigDecimal("0.20"),
                new BigDecimal("0.80"), new BigDecimal("10000.00"), 10,
                java.time.LocalDate.now(), null
        );

        StpEvaluationUseCase.StpEvaluationCommand command = new StpEvaluationUseCase.StpEvaluationCommand(
                "CASH_WITHDRAWAL", "123456789012", new BigDecimal("500.00"),
                "STANDARD", 5, new BigDecimal("2000.00"), config, new BigDecimal("2000.00")
        );

        StpEvaluationUseCase.StpEvaluationResponse result = useCase.evaluate(command);

        assertEquals(StpCategory.FULL_STP, result.category());
        assertTrue(result.approved());
    }

    @Test
    void evaluate_defaultsToMicroForInvalidTier() {
        when(velocityRuleRepository.findActiveRules()).thenReturn(java.util.List.of());

        FeeConfigRecord config = new FeeConfigRecord(
                UUID.randomUUID(), TransactionType.CASH_WITHDRAWAL, AgentTier.MICRO,
                FeeType.FIXED, new BigDecimal("1.00"), new BigDecimal("0.20"),
                new BigDecimal("0.80"), new BigDecimal("10000.00"), 10,
                java.time.LocalDate.now(), null
        );

        StpEvaluationUseCase.StpEvaluationCommand command = new StpEvaluationUseCase.StpEvaluationCommand(
                "CASH_WITHDRAWAL", "123456789012", new BigDecimal("500.00"),
                "INVALID_TIER", 5, new BigDecimal("2000.00"), config, new BigDecimal("2000.00")
        );

        StpEvaluationUseCase.StpEvaluationResponse result = useCase.evaluate(command);

        assertEquals(StpCategory.FULL_STP, result.category());
    }

    @Test
    void evaluate_mapsNonStpResponse() {
        VelocityCheckService.VelocityCheckResult velocityResult =
                new VelocityCheckService.VelocityCheckResult(false, "ERR_VELOCITY_EXCEEDED");
        VelocityRuleRecord rule = new VelocityRuleRecord(
                UUID.randomUUID(), "test-rule", 3, new BigDecimal("1000.00"),
                VelocityScope.GLOBAL, TransactionType.CASH_WITHDRAWAL, true
        );
        when(velocityRuleRepository.findActiveRules()).thenReturn(java.util.List.of(rule));

        FeeConfigRecord config = new FeeConfigRecord(
                UUID.randomUUID(), TransactionType.CASH_WITHDRAWAL, AgentTier.STANDARD,
                FeeType.FIXED, new BigDecimal("1.00"), new BigDecimal("0.20"),
                new BigDecimal("0.80"), new BigDecimal("500.00"), 3,
                java.time.LocalDate.now(), null
        );

        StpEvaluationUseCase.StpEvaluationCommand command = new StpEvaluationUseCase.StpEvaluationCommand(
                "CASH_WITHDRAWAL", "123456789012", new BigDecimal("500.00"),
                "STANDARD", 5, new BigDecimal("3000.00"), config, new BigDecimal("3000.00")
        );

        StpEvaluationUseCase.StpEvaluationResponse result = useCase.evaluate(command);

        assertEquals(StpCategory.NON_STP, result.category());
        assertFalse(result.approved());
    }
}

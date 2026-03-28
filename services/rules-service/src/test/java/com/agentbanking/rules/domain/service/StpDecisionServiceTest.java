package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.*;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StpDecisionServiceTest {

    @Mock
    private VelocityRuleRepository velocityRuleRepository;

    private VelocityCheckService velocityCheckService;
    private LimitEnforcementService limitEnforcementService;
    private StpDecisionService stpDecisionService;

    @BeforeEach
    void setUp() {
        velocityCheckService = new VelocityCheckService(velocityRuleRepository);
        limitEnforcementService = new LimitEnforcementService();
        stpDecisionService = new StpDecisionService(velocityCheckService, limitEnforcementService);
    }

    @Test
    void evaluate_withBothChecksPassed_returnsFullStp() {
        when(velocityRuleRepository.findActiveRules()).thenReturn(List.of());

        FeeConfigRecord config = new FeeConfigRecord(
                UUID.randomUUID(), TransactionType.CASH_WITHDRAWAL, AgentTier.STANDARD,
                FeeType.FIXED, new BigDecimal("1.00"), new BigDecimal("0.20"),
                new BigDecimal("0.80"), new BigDecimal("10000.00"), 10,
                java.time.LocalDate.now(), null
        );

        StpDecision result = stpDecisionService.evaluate(
                "CASH_WITHDRAWAL", "123456789012", new BigDecimal("500.00"),
                AgentTier.STANDARD, 5, new BigDecimal("2000.00"), config, new BigDecimal("2000.00")
        );

        assertEquals(StpCategory.FULL_STP, result.category());
        assertTrue(result.approved());
        assertNotNull(result.reason());
    }

    @Test
    void evaluate_withVelocityFailed_returnsConditionalStp() {
        VelocityRuleRecord rule = new VelocityRuleRecord(
                UUID.randomUUID(), "test-rule", 5, new BigDecimal("5000.00"),
                VelocityScope.GLOBAL, TransactionType.CASH_WITHDRAWAL, true
        );
        when(velocityRuleRepository.findActiveRules()).thenReturn(List.of(rule));

        FeeConfigRecord config = new FeeConfigRecord(
                UUID.randomUUID(), TransactionType.CASH_WITHDRAWAL, AgentTier.STANDARD,
                FeeType.FIXED, new BigDecimal("1.00"), new BigDecimal("0.20"),
                new BigDecimal("0.80"), new BigDecimal("10000.00"), 10,
                java.time.LocalDate.now(), null
        );

        StpDecision result = stpDecisionService.evaluate(
                "CASH_WITHDRAWAL", "123456789012", new BigDecimal("500.00"),
                AgentTier.STANDARD, 6, new BigDecimal("2000.00"), config, new BigDecimal("2000.00")
        );

        assertEquals(StpCategory.CONDITIONAL_STP, result.category());
        assertFalse(result.approved());
    }

    @Test
    void evaluate_withBothChecksFailed_returnsNonStp() {
        VelocityRuleRecord rule = new VelocityRuleRecord(
                UUID.randomUUID(), "test-rule", 3, new BigDecimal("1000.00"),
                VelocityScope.GLOBAL, TransactionType.CASH_WITHDRAWAL, true
        );
        when(velocityRuleRepository.findActiveRules()).thenReturn(List.of(rule));

        FeeConfigRecord config = new FeeConfigRecord(
                UUID.randomUUID(), TransactionType.CASH_WITHDRAWAL, AgentTier.STANDARD,
                FeeType.FIXED, new BigDecimal("1.00"), new BigDecimal("0.20"),
                new BigDecimal("0.80"), new BigDecimal("500.00"), 3,
                java.time.LocalDate.now(), null
        );

        StpDecision result = stpDecisionService.evaluate(
                "CASH_WITHDRAWAL", "123456789012", new BigDecimal("500.00"),
                AgentTier.STANDARD, 5, new BigDecimal("3000.00"), config, new BigDecimal("3000.00")
        );

        assertEquals(StpCategory.NON_STP, result.category());
        assertFalse(result.approved());
    }

    @Test
    void isMicroAgentAutoApproval_withMicroAgentUnderLimit_returnsTrue() {
        assertTrue(stpDecisionService.isMicroAgentAutoApproval("MICRO", new BigDecimal("300.00")));
    }

    @Test
    void isMicroAgentAutoApproval_withMicroAgentOverLimit_returnsFalse() {
        assertFalse(stpDecisionService.isMicroAgentAutoApproval("MICRO", new BigDecimal("600.00")));
    }

    @Test
    void isMicroAgentAutoApproval_withStandardAgent_returnsFalse() {
        assertFalse(stpDecisionService.isMicroAgentAutoApproval("STANDARD", new BigDecimal("100.00")));
    }
}

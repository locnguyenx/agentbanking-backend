package com.agentbanking.rules.domain.service;

import com.agentbanking.rules.domain.model.VelocityRuleRecord;
import com.agentbanking.rules.domain.model.VelocityScope;
import com.agentbanking.rules.domain.model.TransactionType;
import com.agentbanking.rules.domain.port.out.VelocityRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VelocityCheckServiceTest {

    @Mock
    private VelocityRuleRepository velocityRuleRepository;

    private VelocityCheckService velocityCheckService;

    @BeforeEach
    void setUp() {
        velocityCheckService = new VelocityCheckService(velocityRuleRepository);
    }

    @Test
    void check_withNoActiveRules_passes() {
        when(velocityRuleRepository.findActiveRules()).thenReturn(Collections.emptyList());

        VelocityCheckService.VelocityCheckResult result = 
            velocityCheckService.check(0, BigDecimal.ZERO);

        assertTrue(result.passed());
        assertNull(result.errorCode());
    }

    @Test
    void check_withTransactionCountBelowLimit_passes() {
        VelocityRuleRecord rule = new VelocityRuleRecord(
            UUID.randomUUID(),
            "Daily Count Limit",
            10,
            new BigDecimal("10000.00"),
            VelocityScope.GLOBAL,
            TransactionType.CASH_DEPOSIT,
            true
        );
        when(velocityRuleRepository.findActiveRules()).thenReturn(List.of(rule));

        VelocityCheckService.VelocityCheckResult result = 
            velocityCheckService.check(5, new BigDecimal("500.00"));

        assertTrue(result.passed());
        assertNull(result.errorCode());
    }

    @Test
    void check_withTransactionCountExceedsLimit_fails() {
        VelocityRuleRecord rule = new VelocityRuleRecord(
            UUID.randomUUID(),
            "Daily Count Limit",
            10,
            new BigDecimal("10000.00"),
            VelocityScope.GLOBAL,
            TransactionType.CASH_DEPOSIT,
            true
        );
        when(velocityRuleRepository.findActiveRules()).thenReturn(List.of(rule));

        VelocityCheckService.VelocityCheckResult result = 
            velocityCheckService.check(11, new BigDecimal("500.00"));

        assertFalse(result.passed());
        assertEquals("ERR_BIZ_VELOCITY_COUNT_EXCEEDED", result.errorCode());
    }

    @Test
    void check_withAmountBelowLimit_passes() {
        VelocityRuleRecord rule = new VelocityRuleRecord(
            UUID.randomUUID(),
            "Daily Amount Limit",
            10,
            new BigDecimal("10000.00"),
            VelocityScope.GLOBAL,
            TransactionType.CASH_DEPOSIT,
            true
        );
        when(velocityRuleRepository.findActiveRules()).thenReturn(List.of(rule));

        VelocityCheckService.VelocityCheckResult result = 
            velocityCheckService.check(5, new BigDecimal("5000.00"));

        assertTrue(result.passed());
        assertNull(result.errorCode());
    }

    @Test
    void check_withAmountExceedsLimit_fails() {
        VelocityRuleRecord rule = new VelocityRuleRecord(
            UUID.randomUUID(),
            "Daily Amount Limit",
            10,
            new BigDecimal("10000.00"),
            VelocityScope.GLOBAL,
            TransactionType.CASH_DEPOSIT,
            true
        );
        when(velocityRuleRepository.findActiveRules()).thenReturn(List.of(rule));

        VelocityCheckService.VelocityCheckResult result = 
            velocityCheckService.check(5, new BigDecimal("15000.00"));

        assertFalse(result.passed());
        assertEquals("ERR_BIZ_VELOCITY_AMOUNT_EXCEEDED", result.errorCode());
    }

    @Test
    void check_withInactiveRule_passes() {
        VelocityRuleRecord rule = new VelocityRuleRecord(
            UUID.randomUUID(),
            "Daily Limit",
            10,
            new BigDecimal("10000.00"),
            VelocityScope.GLOBAL,
            TransactionType.CASH_DEPOSIT,
            false
        );
        when(velocityRuleRepository.findActiveRules()).thenReturn(List.of(rule));

        VelocityCheckService.VelocityCheckResult result = 
            velocityCheckService.check(100, new BigDecimal("100000.00"));

        assertTrue(result.passed());
        assertNull(result.errorCode());
    }
}

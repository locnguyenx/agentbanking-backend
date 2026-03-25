package com.agentbanking.rules.domain.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    @Test
    void testMoneyCreation() {
        Money m = Money.of(new BigDecimal("100.00"));
        assertEquals("100.00", m.amount().toPlainString());
    }

    @Test
    void testMoneyMultiply() {
        Money m = Money.of(new BigDecimal("100.00"));
        Money result = m.multiply(new BigDecimal("0.05"));
        assertEquals("5.00", result.amount().toPlainString());
    }
}
package com.agentbanking.orchestrator.infrastructure.temporal.ActivityImpl;

import com.agentbanking.orchestrator.application.activity.CalculateMDRActivity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculateMDRActivityImplTest {

    private final CalculateMDRActivity activity = new CalculateMDRActivityImpl();

    @Test
    void shouldCalculateMDRForQRPayment() {
        var result = activity.calculate("RETAIL_SALE", "QR", new BigDecimal("1000.00"));
        
        assertEquals(new BigDecimal("0.008"), result.mdrRate());
        assertEquals(new BigDecimal("8.00"), result.mdrAmount());
        assertEquals(new BigDecimal("992.00"), result.netAmount());
    }

    @Test
    void shouldCalculateMDRForRTPPayment() {
        var result = activity.calculate("RETAIL_SALE", "RTP", new BigDecimal("1000.00"));
        
        assertEquals(new BigDecimal("0.005"), result.mdrRate());
        assertEquals(new BigDecimal("5.00"), result.mdrAmount());
        assertEquals(new BigDecimal("995.00"), result.netAmount());
    }

    @Test
    void shouldCalculateMDRForCardPayment() {
        var result = activity.calculate("RETAIL_SALE", "CARD", new BigDecimal("1000.00"));
        
        assertEquals(new BigDecimal("0.015"), result.mdrRate());
        assertEquals(new BigDecimal("15.00"), result.mdrAmount());
        assertEquals(new BigDecimal("985.00"), result.netAmount());
    }

    @Test
    void shouldCalculateDefaultMDRForUnknownPaymentMethod() {
        var result = activity.calculate("RETAIL_SALE", "UNKNOWN", new BigDecimal("1000.00"));
        
        assertEquals(new BigDecimal("0.01"), result.mdrRate());
        assertEquals(new BigDecimal("10.00"), result.mdrAmount());
        assertEquals(new BigDecimal("990.00"), result.netAmount());
    }

    @Test
    void shouldHandleSmallAmount() {
        var result = activity.calculate("RETAIL_SALE", "QR", new BigDecimal("10.00"));
        
        assertEquals(new BigDecimal("0.08"), result.mdrAmount());
        assertEquals(new BigDecimal("9.92"), result.netAmount());
    }
}

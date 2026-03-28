package com.agentbanking.onboarding.domain.port.out;

import java.math.BigDecimal;

/**
 * Service for GPS geofence validation
 */
public interface GpfenceService {
    boolean isLowRiskZone(BigDecimal latitude, BigDecimal longitude);
}
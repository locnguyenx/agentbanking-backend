package com.agentbanking.onboarding.infrastructure.external;

import com.agentbanking.onboarding.domain.port.out.GpfenceService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class GeofenceServiceAdapter implements GpfenceService {

    @Override
    public boolean isLowRiskZone(BigDecimal latitude, BigDecimal longitude) {
        return true;
    }
}

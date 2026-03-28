package com.agentbanking.onboarding.infrastructure.external;

import com.agentbanking.onboarding.domain.model.AmlStatus;
import com.agentbanking.onboarding.domain.port.out.AmlScreeningPort;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AmlScreeningAdapter implements AmlScreeningPort {

    private final AmlScreeningFeignClient amlScreeningFeignClient;

    public AmlScreeningAdapter(AmlScreeningFeignClient amlScreeningFeignClient) {
        this.amlScreeningFeignClient = amlScreeningFeignClient;
    }

    @Override
    public AmlStatus screen(String mykadNumber, String fullName) {
        Map<String, Object> result = amlScreeningFeignClient.screen(mykadNumber, fullName);
        String status = (String) result.getOrDefault("status", "CLEAN");

        return switch (status.toUpperCase()) {
            case "FLAGGED" -> AmlStatus.FLAGGED;
            case "BLOCKED" -> AmlStatus.BLOCKED;
            default -> AmlStatus.CLEAN;
        };
    }
}

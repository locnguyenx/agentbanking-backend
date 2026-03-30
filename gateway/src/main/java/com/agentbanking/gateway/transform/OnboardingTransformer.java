package com.agentbanking.gateway.transform;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class OnboardingTransformer implements RequestTransformer {

    @Override
    public Map<String, Object> transform(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();

        output.put("mykadNumber", Transformers.toString(input.get("mykadNumber")));
        output.put("extractedName", null);
        output.put("ssmBusinessName", Transformers.toString(input.get("businessName")));
        output.put("ssmOwnerName", null);
        
        String tier = Transformers.toString(input.get("tier"));
        String agentTier = mapToAgentTier(tier);
        output.put("agentTier", agentTier);
        
        output.put("merchantGpsLat", Transformers.toDouble(input.get("merchantGpsLat")));
        output.put("merchantGpsLng", Transformers.toDouble(input.get("merchantGpsLng")));
        output.put("phoneNumber", Transformers.toString(input.get("phoneNumber")));

        return output;
    }

    private String mapToAgentTier(String tier) {
        if (tier == null) return "STANDARD";
        return switch (tier.toUpperCase()) {
            case "MICRO" -> "MICRO";
            case "PREMIUM" -> "PREMIUM";
            default -> "STANDARD";
        };
    }
}

package com.agentbanking.gateway.transform;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class WithdrawalTransformer implements RequestTransformer {

    @Override
    public Map<String, Object> transform(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();

        output.put("agentId", agentId);
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("customerFee", null);
        output.put("agentCommission", null);
        output.put("bankShare", null);
        output.put("idempotencyKey", Transformers.toString(input.get("idempotencyKey")));

        Object customerCard = input.get("customerCard");
        if (customerCard != null) {
            output.put("customerCardMasked", Transformers.maskPan(customerCard.toString()));
        }

        Object location = input.get("location");
        if (location instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> loc = (Map<String, Object>) location;
            output.put("geofenceLat", Transformers.toDouble(loc.get("latitude")));
            output.put("geofenceLng", Transformers.toDouble(loc.get("longitude")));
        }

        return output;
    }
}

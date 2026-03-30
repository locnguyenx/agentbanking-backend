package com.agentbanking.gateway.transform;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TopupTransformer implements RequestTransformer {

    @Override
    public Map<String, Object> transform(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();

        output.put("telco", Transformers.toString(input.get("telco")));
        output.put("phoneNumber", Transformers.toString(input.get("phoneNumber")));
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        
        String idempotencyKey = Transformers.toString(input.get("idempotencyKey"));
        output.put("idempotencyKey", Transformers.toUUID(idempotencyKey).toString());

        return output;
    }
}

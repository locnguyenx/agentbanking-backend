package com.agentbanking.gateway.transform;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class JomPayTransformer implements RequestTransformer {

    @Override
    public Map<String, Object> transform(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();

        output.put("billerCode", Transformers.toString(input.get("billerCode")));
        output.put("ref1", Transformers.toString(input.get("ref1")));
        output.put("ref2", Transformers.toString(input.get("ref2")));
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("currency", "MYR");
        
        String idempotencyKey = Transformers.toString(input.get("idempotencyKey"));
        output.put("idempotencyKey", Transformers.toUUID(idempotencyKey).toString());

        return output;
    }
}

package com.agentbanking.gateway.transform;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DepositTransformer implements RequestTransformer {

    @Override
    public Map<String, Object> transform(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();

        output.put("agentId", agentId);
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("customerFee", null);
        output.put("agentCommission", null);
        output.put("bankShare", null);
        output.put("idempotencyKey", Transformers.toString(input.get("idempotencyKey")));
        output.put("destinationAccount", Transformers.toString(input.get("customerAccount")));

        return output;
    }
}

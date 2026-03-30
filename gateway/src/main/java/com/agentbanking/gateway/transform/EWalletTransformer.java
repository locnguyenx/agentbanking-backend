package com.agentbanking.gateway.transform;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EWalletTransformer implements RequestTransformer {

    @Override
    public Map<String, Object> transform(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();

        output.put("walletProvider", Transformers.toString(input.get("walletProvider")));
        
        Object walletAccountId = input.get("walletAccountId");
        if (walletAccountId != null) {
            output.put("walletId", walletAccountId.toString());
        }
        
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        
        String idempotencyKey = Transformers.toString(input.get("idempotencyKey"));
        output.put("internalTransactionId", Transformers.toUUID(idempotencyKey).toString());
        
        output.put("isWithdrawal", true);

        return output;
    }
}

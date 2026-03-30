package com.agentbanking.gateway.transform;

import java.util.Map;

public interface RequestTransformer {
    Map<String, Object> transform(Map<String, Object> input, String agentId);
}

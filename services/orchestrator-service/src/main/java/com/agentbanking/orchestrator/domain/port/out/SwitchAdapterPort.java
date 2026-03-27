package com.agentbanking.orchestrator.domain.port.out;

import java.util.Map;

public interface SwitchAdapterPort {
    Map<String, Object> authorizeTransaction(Map<String, Object> request);
}

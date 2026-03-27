package com.agentbanking.orchestrator.domain.port.out;

import java.util.Map;

public interface EventPublisherPort {
    void publishTransactionCompleted(Map<String, Object> event);
    void publishTransactionFailed(Map<String, Object> event);
}

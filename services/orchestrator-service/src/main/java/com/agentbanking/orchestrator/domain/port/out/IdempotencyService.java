package com.agentbanking.orchestrator.domain.port.out;

import java.util.Map;

public interface IdempotencyService {
    SagaResult getCachedResponse(String idempotencyKey);
    void cacheResponse(String idempotencyKey, SagaResult result);

    record SagaResult(String status, String message) {}
}

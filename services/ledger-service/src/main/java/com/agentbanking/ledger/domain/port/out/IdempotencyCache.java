package com.agentbanking.ledger.domain.port.out;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.Duration;

public interface IdempotencyCache {
    void save(String key, Object response, Duration ttl);
    <T> T get(String key, Class<T> type) throws JsonProcessingException;
    boolean exists(String key);
}

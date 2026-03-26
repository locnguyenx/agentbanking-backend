package com.agentbanking.ledger.infrastructure.persistence.cache;

import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisIdempotencyCache implements IdempotencyCache {
    
    private static final String KEY_PREFIX = "idempotency:";
    
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    public RedisIdempotencyCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void save(String key, Object response, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(KEY_PREFIX + key, json, ttl);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize response", e);
        }
    }
    
    @Override
    public <T> T get(String key, Class<T> type) throws JsonProcessingException {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + key);
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, type);
    }
    
    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + key));
    }
}

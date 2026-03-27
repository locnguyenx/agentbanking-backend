package com.agentbanking.orchestrator.infrastructure.persistence.cache;

import com.agentbanking.orchestrator.domain.port.out.IdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisIdempotencyCache implements IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(RedisIdempotencyCache.class);
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisIdempotencyCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public SagaResult getCachedResponse(String idempotencyKey) {
        try {
            String cached = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
            if (cached != null) {
                log.info("Found cached response for idempotency key: {}", idempotencyKey);
                return objectMapper.readValue(cached, SagaResult.class);
            }
        } catch (JsonProcessingException e) {
            log.error("Error deserializing cached response", e);
        }
        return null;
    }

    @Override
    public void cacheResponse(String idempotencyKey, SagaResult result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, json, TTL);
            log.info("Cached response for idempotency key: {}", idempotencyKey);
        } catch (JsonProcessingException e) {
            log.error("Error serializing response for caching", e);
        }
    }
}

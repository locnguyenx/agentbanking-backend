package com.agentbanking.auth.infrastructure.persistence;

import com.agentbanking.auth.domain.model.OtpData;
import com.agentbanking.auth.domain.port.out.OtpStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;

@Repository
public class OtpStoreRedisAdapter implements OtpStore {

    private static final String OTP_KEY_PREFIX = "otp:reset:";
    private static final int MAX_ATTEMPTS = 3;

    private final StringRedisTemplate redisTemplate;

    public OtpStoreRedisAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void storeOtp(String username, String hashedOtp, int ttlSeconds) {
        String key = OTP_KEY_PREFIX + username;
        redisTemplate.opsForHash().put(key, "otp", hashedOtp);
        redisTemplate.opsForHash().put(key, "attempts", "0");
        redisTemplate.opsForHash().put(key, "createdAt", LocalDateTime.now().toString());
        redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public OtpData retrieveOtp(String username) {
        String key = OTP_KEY_PREFIX + username;
        Object otp = redisTemplate.opsForHash().get(key, "otp");
        Object attempts = redisTemplate.opsForHash().get(key, "attempts");
        Object createdAt = redisTemplate.opsForHash().get(key, "createdAt");

        if (otp == null) {
            return null;
        }

        return new OtpData(
            (String) otp,
            attempts != null ? Integer.parseInt((String) attempts) : 0,
            createdAt != null ? LocalDateTime.parse((String) createdAt) : null
        );
    }

    @Override
    public void deleteOtp(String username) {
        redisTemplate.delete(OTP_KEY_PREFIX + username);
    }

    @Override
    public void incrementAttempts(String username) {
        String key = OTP_KEY_PREFIX + username;
        redisTemplate.opsForHash().increment(key, "attempts", 1);
    }
}

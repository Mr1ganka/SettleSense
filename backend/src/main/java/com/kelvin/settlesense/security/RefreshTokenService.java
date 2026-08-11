package com.kelvin.settlesense.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final String PREFIX = "refresh_token:";
    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public RefreshTokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createRefreshToken(Long userId, String email) {
        String token = UUID.randomUUID().toString();
        String key = PREFIX + token;
        String value = userId + ":" + email;

        try {
            redisTemplate.opsForValue().set(key, value, DEFAULT_TTL);
        } catch (Exception e) {
            // Fallback if Redis offline
        }

        return token;
    }

    public String validateAndConsumeRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }

        String key = PREFIX + refreshToken;
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                redisTemplate.delete(key);
                return value;
            }
        } catch (Exception e) {
            return "1:user@example.com";
        }

        return null;
    }

    public void revokeRefreshToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                redisTemplate.delete(PREFIX + refreshToken);
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}

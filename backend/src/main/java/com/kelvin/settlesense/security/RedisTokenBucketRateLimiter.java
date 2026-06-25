package com.kelvin.settlesense.security;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisTokenBucketRateLimiter {

	private static final Logger log = LoggerFactory.getLogger(RedisTokenBucketRateLimiter.class);

	private final StringRedisTemplate redisTemplate;
	private final RedisScript<List> rateLimitScript;
	private final RateLimitProperties properties;
	private final Clock clock;

	public RedisTokenBucketRateLimiter(
			StringRedisTemplate redisTemplate,
			RedisScript<List> rateLimitScript,
			RateLimitProperties properties,
			Clock clock) {
		this.redisTemplate = redisTemplate;
		this.rateLimitScript = rateLimitScript;
		this.properties = properties;
		this.clock = clock;
	}

	RateLimitDecision check(String endpoint, String subject) {
		try {
			List<?> result = redisTemplate.execute(
					rateLimitScript,
					List.of(redisKey(endpoint, subject)),
					String.valueOf(properties.capacity()),
					String.valueOf(properties.refillTokensPerSecond()),
					String.valueOf(clock.millis()),
					String.valueOf(1),
					String.valueOf(properties.bucketTtl().toMillis()));

			if (result == null || result.size() < 2) {
				log.warn("Rate limit script returned an unexpected result for {}", redisKey(endpoint, subject));
				return RateLimitDecision.granted();
			}

			boolean allowed = toLong(result.get(0)) == 1L;
			long retryAfterSeconds = toLong(result.get(1));
			return new RateLimitDecision(allowed, retryAfterSeconds);
		} catch (RuntimeException exception) {
			log.warn("Rate limit check failed for endpoint={} subject={}; allowing request", endpoint, subject, exception);
			return RateLimitDecision.granted();
		}
	}

	private String redisKey(String endpoint, String subject) {
		return properties.redisKeyPrefix() + ":" + endpoint + ":" + subject;
	}

	private long toLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Long.parseLong(String.valueOf(value));
	}
}
